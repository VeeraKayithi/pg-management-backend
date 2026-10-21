$ErrorActionPreference = 'Stop'

# Run this script from the root of pg-management-backend.
$root = Join-Path (Get-Location) 'src/main/java/com/pgcompliance'
$repoPath = Join-Path $root 'repository/RoomRepository.java'
$tenantPath = Join-Path $root 'service/impl/TenantServiceImpl.java'
$roomPath = Join-Path $root 'service/impl/RoomServiceImpl.java'
foreach ($path in @($repoPath, $tenantPath, $roomPath)) {
    if (-not (Test-Path $path)) { throw "Missing expected source file: $path. Run from backend root." }
}

function Replace-Once([string]$s, [string]$old, [string]$new, [string]$label) {
    $first = $s.IndexOf($old, [StringComparison]::Ordinal)
    if ($first -lt 0) { throw "Expected code not found: $label. No files changed." }
    if ($s.IndexOf($old, $first + $old.Length, [StringComparison]::Ordinal) -ge 0) { throw "Ambiguous multiple matches: $label. No files changed." }
    return $s.Substring(0,$first) + $new + $s.Substring($first + $old.Length)
}

$repo = [IO.File]::ReadAllText($repoPath)
$tenant = [IO.File]::ReadAllText($tenantPath)
$room = [IO.File]::ReadAllText($roomPath)

if ($repo.Contains('findByIdForUpdate(') -or $tenant.Contains('findByIdForUpdate(')) {
    throw 'Locking code appears to be present already; refusing to reapply.'
}

$repo = Replace-Once $repo `
    'import org.springframework.data.jpa.repository.JpaRepository;' `
    "import jakarta.persistence.LockModeType;`nimport org.springframework.data.jpa.repository.JpaRepository;`nimport org.springframework.data.jpa.repository.Lock;`nimport org.springframework.data.jpa.repository.Query;`nimport org.springframework.data.repository.query.Param;`nimport java.util.Optional;" 'RoomRepository imports'

$repo = Replace-Once $repo `
    '    boolean existsByBuildingBuildingId(Long buildingId);' `
    @'
    // Call only inside an active @Transactional method.
    // PostgreSQL SELECT ... FOR UPDATE serializes assignments to a room.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.roomId = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

    boolean existsByBuildingBuildingId(Long buildingId);
'@ 'RoomRepository locking query'

# All tenant room lookups during create/update now lock the destination
# before occupancy checks or assignment mutations.
$tenant = Replace-Once $tenant `
    @'
        private Room findRoom(
                        Long roomId) {

                return roomRepository
                                .findById(
                                                roomId)
'@ `
    @'
        private Room findRoom(
                        Long roomId) {

                return roomRepository
                                .findByIdForUpdate(
                                                roomId)
'@ 'TenantServiceImpl.findRoom lock'

# Serialize room-edit operations with tenant assignment checks.
$room = Replace-Once $room `
    @'
        public RoomResponseDto updateRoom(
                        Long roomId,
                        RoomRequestDto request) {

                Room room = roomRepository
                                .findById(roomId)
'@ `
    @'
        public RoomResponseDto updateRoom(
                        Long roomId,
                        RoomRequestDto request) {

                Room room = roomRepository
                                .findByIdForUpdate(roomId)
'@ 'RoomServiceImpl.updateRoom lock'


# Protect against reducing room capacity below current active resident count.
$room = Replace-Once $room `
    @'
                room.setSharingType(
                                request.getSharingType());
'@ `
    @'
                int requestedCapacity = switch (request.getSharingType()) {
                        case SINGLE -> 1;
                        case DOUBLE -> 2;
                        case TRIPLE -> 3;
                        case FOUR -> 4;
                };
                long activeResidents = tenantRepository.countByRoomRoomIdAndTenantStatus(
                                roomId, com.pgcompliance.constant.TenantStatus.ACTIVE);
                if (activeResidents > requestedCapacity) {
                        throw new IllegalStateException(
                                        "Cannot reduce room capacity below active occupancy");
                }

                room.setSharingType(
                                request.getSharingType());
'@ 'RoomServiceImpl capacity reduction guard'

# Preserve CRLF if original sources use it.
$changes = @(
    @{path=$repoPath; content=$repo},
    @{path=$tenantPath; content=$tenant},
    @{path=$roomPath; content=$room}
)
$backupRoot = Join-Path $env:TEMP 'nandu-a01-backup'
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
foreach ($change in $changes) {
    Copy-Item $change.path (Join-Path $backupRoot ([IO.Path]::GetFileName($change.path))) -Force
    [IO.File]::WriteAllText($change.path, $change.content, [Text.UTF8Encoding]::new($false))
}
Write-Host 'A01 applied to RoomRepository, TenantServiceImpl, RoomServiceImpl.' -ForegroundColor Green
Write-Host "Original files backed up to $backupRoot" -ForegroundColor Green
Write-Host 'Next: git diff; .\mvnw.cmd test' -ForegroundColor Cyan