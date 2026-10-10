package com.pgcompliance.service.impl;

import com.pgcompliance.constant.*;
import com.pgcompliance.dto.*;
import com.pgcompliance.entity.*;
import com.pgcompliance.exception.ResourceNotFoundException;
import com.pgcompliance.repository.*;
import com.pgcompliance.service.AnnouncementService;
import com.pgcompliance.service.EmailService;
import com.pgcompliance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDeliveryRepository deliveryRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${app.frontend.tenant-dashboard-url:http://localhost:5173/tenant/dashboard}")
    private String tenantDashboardUrl;

    @Override
    @Transactional(readOnly = true)
    public AnnouncementPreviewResponseDto preview(
            AnnouncementPreviewRequestDto request) {

        List<Tenant> tenants = activeTenants();

        boolean wantsInApp = request.getChannels().contains(
                CommunicationChannel.IN_APP);

        boolean wantsEmail = request.getChannels().contains(
                CommunicationChannel.EMAIL);

        long portalAccountTenantCount = 0;
        long eligibleTenantCount = 0;
        long inAppEligibleCount = 0;
        long emailEligibleCount = 0;
        long tenantsWithoutPortalAccount = 0;

        for (Tenant tenant : tenants) {

            Optional<User> linkedUser = userRepository
                    .findByTenantTenantId(
                            tenant.getTenantId());

            if (linkedUser.isEmpty()) {

                tenantsWithoutPortalAccount++;
                continue;
            }

            portalAccountTenantCount++;

            User user = linkedUser.get();

            boolean portalActive = Boolean.TRUE.equals(
                    user.getActive());

            boolean verifiedEmailAvailable = portalActive
                    && Boolean.TRUE.equals(
                            user.getEmailVerified())
                    && tenant.getEmail() != null
                    && !tenant.getEmail().isBlank();

            boolean inAppEligible = wantsInApp
                    && portalActive;

            boolean emailEligible = wantsEmail
                    && verifiedEmailAvailable;

            if (inAppEligible) {
                inAppEligibleCount++;
            }

            if (emailEligible) {
                emailEligibleCount++;
            }

            /*
             * Count the Tenant once when at least one selected
             * channel can be delivered.
             */
            if (inAppEligible || emailEligible) {
                eligibleTenantCount++;
            }
        }

        return AnnouncementPreviewResponseDto
                .builder()
                .activeTenantCount(
                        tenants.size())
                .portalAccountTenantCount(
                        portalAccountTenantCount)
                .eligibleTenantCount(
                        eligibleTenantCount)
                .inAppEligibleCount(
                        inAppEligibleCount)
                .emailEligibleCount(
                        emailEligibleCount)
                .inAppSkippedCount(
                        wantsInApp
                                ? tenants.size()
                                        - inAppEligibleCount
                                : 0)
                .emailSkippedCount(
                        wantsEmail
                                ? tenants.size()
                                        - emailEligibleCount
                                : 0)
                .tenantsWithoutPortalAccount(
                        tenantsWithoutPortalAccount)
                .build();
    }

    @Override
    public AnnouncementResponseDto send(
            AnnouncementCreateRequestDto request,
            String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin account not found"));

        Set<CommunicationChannel> channels = request.getChannels();
        Announcement announcement = Announcement.builder()
                .title(normalize(request.getTitle()))
                .message(normalizeMultiline(request.getMessage()))
                .priority(request.getPriority())
                .sendInApp(channels.contains(CommunicationChannel.IN_APP))
                .sendEmail(channels.contains(CommunicationChannel.EMAIL))
                .deepLink(normalizeDeepLink(request.getDeepLink()))
                .createdBy(admin)
                .status(AnnouncementStatus.PROCESSING)
                .targetTenantCount(activeTenants().size())
                .sentCount(0)
                .failedCount(0)
                .skippedCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        announcement = announcementRepository.saveAndFlush(announcement);

        int sent = 0;
        int failed = 0;
        int skipped = 0;

        for (Tenant tenant : activeTenants()) {
            Optional<User> linkedUser = userRepository.findByTenantTenantId(tenant.getTenantId());

            if (Boolean.TRUE.equals(announcement.getSendInApp())) {
                CommunicationDeliveryStatus result = sendInApp(announcement, tenant, linkedUser.orElse(null));
                if (result == CommunicationDeliveryStatus.SENT)
                    sent++;
                else if (result == CommunicationDeliveryStatus.FAILED)
                    failed++;
                else
                    skipped++;
            }

            if (Boolean.TRUE.equals(announcement.getSendEmail())) {
                CommunicationDeliveryStatus result = sendEmail(announcement, tenant, linkedUser.orElse(null));
                if (result == CommunicationDeliveryStatus.SENT)
                    sent++;
                else if (result == CommunicationDeliveryStatus.FAILED)
                    failed++;
                else
                    skipped++;
            }
        }

        announcement.setSentCount(sent);
        announcement.setFailedCount(failed);
        announcement.setSkippedCount(skipped);
        announcement.setSentAt(LocalDateTime.now());
        announcement.setStatus(resolveStatus(sent, failed, skipped));

        return mapAnnouncement(announcementRepository.save(announcement));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> getHistory() {
        return announcementRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::mapAnnouncement).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementDeliveryResponseDto> getDeliveries(Long announcementId) {
        if (!announcementRepository.existsById(announcementId)) {
            throw new ResourceNotFoundException("Announcement not found");
        }
        return deliveryRepository
                .findByAnnouncementAnnouncementIdOrderByCreatedAtAsc(announcementId)
                .stream().map(this::mapDelivery).toList();
    }

    private CommunicationDeliveryStatus sendInApp(
            Announcement announcement,
            Tenant tenant,
            User user) {
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            saveDelivery(announcement, tenant, user, CommunicationChannel.IN_APP,
                    CommunicationDeliveryStatus.SKIPPED,
                    "Active portal account unavailable");
            return CommunicationDeliveryStatus.SKIPPED;
        }

        try {
            notificationService.createNotification(
                    user.getUserId(),
                    NotificationEventType.ADMIN_ANNOUNCEMENT,
                    mapSeverity(announcement.getPriority()),
                    "ANNOUNCEMENT",
                    announcement.getTitle(),
                    announcement.getMessage(),
                    announcement.getDeepLink(),
                    "ANNOUNCEMENT",
                    announcement.getAnnouncementId(),
                    "ADMIN_ANNOUNCEMENT:" + announcement.getAnnouncementId() + ":" + user.getUserId());
            saveDelivery(announcement, tenant, user, CommunicationChannel.IN_APP,
                    CommunicationDeliveryStatus.SENT, null);
            return CommunicationDeliveryStatus.SENT;
        } catch (Exception exception) {
            saveDelivery(announcement, tenant, user, CommunicationChannel.IN_APP,
                    CommunicationDeliveryStatus.FAILED, safeReason(exception));
            return CommunicationDeliveryStatus.FAILED;
        }
    }

    private CommunicationDeliveryStatus sendEmail(
            Announcement announcement,
            Tenant tenant,
            User user) {
        if (user == null
                || !Boolean.TRUE.equals(user.getActive())
                || !Boolean.TRUE.equals(user.getEmailVerified())
                || tenant.getEmail() == null
                || tenant.getEmail().isBlank()) {
            saveDelivery(announcement, tenant, user, CommunicationChannel.EMAIL,
                    CommunicationDeliveryStatus.SKIPPED,
                    "Verified active email account unavailable");
            return CommunicationDeliveryStatus.SKIPPED;
        }

        try {
            String details = "Priority: " + announcement.getPriority()
                    + "<br>Sent: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            emailService.sendEmail(EmailTemplateData.builder()
                    .recipientEmail(tenant.getEmail())
                    .recipientName(tenant.getName())
                    .emailTitle(announcement.getTitle())
                    .emailCategory("PG Announcement")
                    .emailHeading(announcement.getTitle())
                    .emailSummary("An operational update from Nandu PG Management.")
                    .emailBody(announcement.getMessage())
                    .detailsTitle("Announcement Details")
                    .emailDetails(details)
                    .actionLabel("Open Tenant Portal")
                    .actionUrl(tenantDashboardUrl)
                    .build());

            saveDelivery(announcement, tenant, user, CommunicationChannel.EMAIL,
                    CommunicationDeliveryStatus.SENT, null);
            return CommunicationDeliveryStatus.SENT;
        } catch (Exception exception) {
            saveDelivery(announcement, tenant, user, CommunicationChannel.EMAIL,
                    CommunicationDeliveryStatus.FAILED, safeReason(exception));
            return CommunicationDeliveryStatus.FAILED;
        }
    }

    private void saveDelivery(
            Announcement announcement,
            Tenant tenant,
            User user,
            CommunicationChannel channel,
            CommunicationDeliveryStatus status,
            String reason) {
        LocalDateTime now = LocalDateTime.now();
        deliveryRepository.save(AnnouncementDelivery.builder()
                .announcement(announcement)
                .tenant(tenant)
                .user(user)
                .channel(channel)
                .status(status)
                .failureReason(reason)
                .sentAt(status == CommunicationDeliveryStatus.SENT ? now : null)
                .failedAt(status == CommunicationDeliveryStatus.FAILED ? now : null)
                .createdAt(now)
                .build());
    }

    private List<Tenant> activeTenants() {
        return tenantRepository.findByTenantStatusOrderByNameAsc(TenantStatus.ACTIVE);
    }

    private NotificationSeverity mapSeverity(AnnouncementPriority priority) {
        return switch (priority) {
            case NORMAL -> NotificationSeverity.INFO;
            case IMPORTANT -> NotificationSeverity.WARNING;
            case URGENT -> NotificationSeverity.ERROR;
        };
    }

    private AnnouncementStatus resolveStatus(int sent, int failed, int skipped) {
        if (sent == 0 && failed > 0)
            return AnnouncementStatus.FAILED;
        if (failed > 0 || skipped > 0)
            return AnnouncementStatus.PARTIALLY_FAILED;
        return AnnouncementStatus.COMPLETED;
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeMultiline(String value) {
        return value.trim().replaceAll("[ \\t]+", " ");
    }

    private String normalizeDeepLink(String value) {
        if (value == null || value.isBlank())
            return "/tenant/dashboard";
        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            throw new IllegalStateException("Deep link must start with /");
        }
        return normalized;
    }

    private String safeReason(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank())
            return "Delivery failed";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private AnnouncementResponseDto mapAnnouncement(Announcement a) {
        return AnnouncementResponseDto.builder()
                .announcementId(a.getAnnouncementId())
                .title(a.getTitle())
                .message(a.getMessage())
                .priority(a.getPriority())
                .sendInApp(a.getSendInApp())
                .sendEmail(a.getSendEmail())
                .deepLink(a.getDeepLink())
                .createdByUsername(a.getCreatedBy().getUsername())
                .status(a.getStatus())
                .targetTenantCount(a.getTargetTenantCount())
                .sentCount(a.getSentCount())
                .failedCount(a.getFailedCount())
                .skippedCount(a.getSkippedCount())
                .createdAt(a.getCreatedAt())
                .sentAt(a.getSentAt())
                .build();
    }

    private AnnouncementDeliveryResponseDto mapDelivery(AnnouncementDelivery d) {
        return AnnouncementDeliveryResponseDto.builder()
                .deliveryId(d.getDeliveryId())
                .tenantId(d.getTenant().getTenantId())
                .tenantName(d.getTenant().getName())
                .email(d.getTenant().getEmail())
                .username(d.getUser() == null ? null : d.getUser().getUsername())
                .channel(d.getChannel())
                .status(d.getStatus())
                .failureReason(d.getFailureReason())
                .sentAt(d.getSentAt())
                .failedAt(d.getFailedAt())
                .build();
    }
}
