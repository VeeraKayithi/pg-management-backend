$ErrorActionPreference = 'Stop'
$path = 'src/main/java/com/pgcompliance/service/impl/TenantServiceImpl.java'
if (!(Test-Path 'pom.xml') -or !(Test-Path $path)) { throw 'Run this script from the backend project root (next to pom.xml).' }
if (!(Test-Path 'src/main/java/com/pgcompliance/entity/TenantStay.java') -or !(Test-Path 'src/main/java/com/pgcompliance/repository/TenantStayRepository.java')) { throw 'Stage 1 TenantStay entity/repository are missing. Stop and copy Stage 1 files first.' }
$original = [System.IO.File]::ReadAllText((Resolve-Path $path).Path)
$code = $original.Replace("`r`n", "`n")
if ($code.Contains('private final TenantStayRepository tenantStayRepository;')) { throw 'Stage 3 appears already applied; no file changed.' }
function Replace-Once([string]$old, [string]$new, [string]$description) {
  $tokens = [regex]::Split($old.Trim(), '\s+')
  $pattern = ($tokens | ForEach-Object { [regex]::Escape($_) }) -join '\s+'
  $matches = [regex]::Matches($script:code, $pattern)
  if ($matches.Count -ne 1) {
    throw "Expected exactly one source match for $description; found $($matches.Count). No changes written."
  }
  $regex = [regex]::new($pattern)
  $script:code = $regex.Replace($script:code, [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $new.Trim() }, 1)
}
Replace-Once 'import com.pgcompliance.entity.Tenant;' @'
import com.pgcompliance.entity.Tenant;
import com.pgcompliance.entity.TenantStay;
import com.pgcompliance.entity.RoomTransferHistory;
'@ 'new entity imports'
Replace-Once 'import com.pgcompliance.repository.TenantRepository;' @'
import com.pgcompliance.repository.TenantRepository;
import com.pgcompliance.repository.TenantStayRepository;
import com.pgcompliance.repository.RoomTransferHistoryRepository;
'@ 'new repository imports'
Replace-Once '        private final TenantRepository tenantRepository;' @'
        private final TenantRepository tenantRepository;
        private final TenantStayRepository tenantStayRepository;
        private final RoomTransferHistoryRepository roomTransferHistoryRepository;
'@ 'repository dependencies'
# On reactivation, the original stay is already COMPLETED. Create a new ACTIVE record.
Replace-Once @'
                        Tenant reactivatedTenant = tenantRepository.saveAndFlush(
                                        existingTenant);
'@ @'
                        Tenant reactivatedTenant = tenantRepository.saveAndFlush(
                                        existingTenant);
                        createActiveStay(reactivatedTenant, requestedRoom);
'@ 'returning tenant new stay'
Replace-Once @'
                Tenant savedTenant = tenantRepository.saveAndFlush(
                                tenant);
'@ @'
                Tenant savedTenant = tenantRepository.saveAndFlush(
                                tenant);
                createActiveStay(savedTenant, requestedRoom);
'@ 'new tenant stay'
# Only an active stay tracks an editable current joining date and room.
Replace-Once @'
                Tenant updatedTenant = tenantRepository.saveAndFlush(
                                tenant);
'@ @'
                Tenant updatedTenant = tenantRepository.saveAndFlush(
                                tenant);
                if (updatedTenant.getTenantStatus() == TenantStatus.ACTIVE) {
                        TenantStay activeStay = requireActiveStay(updatedTenant.getTenantId());
                        activeStay.setJoiningDate(updatedTenant.getJoiningDate());
                        if (roomChanged) {
                                roomTransferHistoryRepository.save(RoomTransferHistory.builder()
                                                .stay(activeStay)
                                                .fromRoom(oldRoom)
                                                .toRoom(requestedRoom)
                                                .transferredAt(LocalDateTime.now())
                                                .build());
                                activeStay.setRoom(requestedRoom);
                        }
                        tenantStayRepository.save(activeStay);
                }
'@ 'edit + room transfer history'
Replace-Once @'
                tenantRepository.saveAndFlush(
                                tenant);

                if (assignedRoom != null) {
'@ @'
                tenantRepository.saveAndFlush(
                                tenant);
                TenantStay activeStay = requireActiveStay(tenant.getTenantId());
                activeStay.setStayStatus(TenantStay.StayStatus.COMPLETED);
                // This API does not collect an actual checkout date. Do not fabricate one.
                activeStay.setLeavingDate(null);
                tenantStayRepository.save(activeStay);

                if (assignedRoom != null) {
'@ 'complete stay on departure'
Replace-Once @'
        private Tenant findTenant(
'@ @'
        private TenantStay requireActiveStay(Long tenantId) {
                return tenantStayRepository
                                .findByTenantTenantIdAndStayStatus(tenantId, TenantStay.StayStatus.ACTIVE)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Active stay missing for tenant " + tenantId
                                                                + ". Check A02 backfill before making changes."));
        }

        private void createActiveStay(Tenant tenant, Room room) {
                if (tenantStayRepository.findByTenantTenantIdAndStayStatus(
                                tenant.getTenantId(), TenantStay.StayStatus.ACTIVE).isPresent()) {
                        throw new IllegalStateException("Tenant already has an active stay");
                }
                tenantStayRepository.save(TenantStay.builder()
                                .tenant(tenant)
                                .room(room)
                                .joiningDate(tenant.getJoiningDate())
                                .stayStatus(TenantStay.StayStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .build());
        }

        private Tenant findTenant(
'@ 'stay helper methods'
$backup = "$path.a02stage3.bak"
if (Test-Path $backup) { throw "Backup already exists at $backup; please rename it before reapplying." }
[System.IO.File]::WriteAllText((Join-Path (Get-Location) $backup), $original, (New-Object System.Text.UTF8Encoding $false))
[System.IO.File]::WriteAllText((Join-Path (Get-Location) $path), $code, (New-Object System.Text.UTF8Encoding $false))
Write-Host 'A02 Stage 3 patch applied. TenantServiceImpl.java backed up as .a02stage3.bak.' -ForegroundColor Green
Write-Host 'Next: .\mvnw.cmd test   ;   git diff --stat' -ForegroundColor Cyan
