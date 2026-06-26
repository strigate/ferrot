# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.9.3 - 2026-06-26
### Changed
- Updated About screen external links and social link presentation
- Prepared localization resources by separating fixed, non-translatable values
- Updated AndroidX Lifecycle, Compose BOM, Firebase BOM, and Gradle wrapper versions
- Added worker unit test coverage for download and cleanup background work

### Fixed
- Corrected restored download scroll behavior near the top of the downloads list

## 1.9.2 - 2026-06-16
### Changed
- Refined background work organization by splitting scheduling and update handling into dedicated use cases
- Extracted the downloads top app bar and simplified download ID and SHA-256 utility handling
- Removed redundant manual WorkManager initialization and updated release artifact task naming
- Updated Firebase BOM and Byte Buddy and KSP build dependencies

### Fixed
- Added missing divider sizing to shared Refinery dimens

## 1.9.1 - 2026-05-23
### Changed
- Removed the unused `PAUSED` download status and simplified related download ordering and UI handling
- Updated the Compose BOM and Gradle wrapper

### Fixed
- Hardened resumable download requeue recovery and skipped downloads that already have tracked running work
- Corrected combined delete success reporting when related cleanup rows are already missing
- Scoped `Retry failed` to visible, non-pending downloads that match the current filter
- Reordered Firebase property application in the app Gradle configuration

## 1.9.0 - 2026-05-17
### Added
- Selected download size in the downloads selection app bar

### Changed
- Improved internal UI component and theme organization
- Updated build and test dependencies

### Fixed
- Improved the share target label shown by the system
- Hardened duplicate and orphan download cleanup to better protect active downloads
- Simplified downloads list auto-scroll behavior around newly visible and restored items
- Preserved the search cursor position when the downloads query is trimmed
- Simplified automatic app update scheduling and legacy periodic work cleanup

## 1.8.1 - 2026-05-09
### Fixed
- Preserved the legacy default swipe directions when swipe action preferences are missing or invalid
- Unified swipe action fallback handling to use typed configured defaults consistently

## 1.8.0 - 2026-05-09
### Added
- Configurable left and right swipe actions for downloads
- Swipe action settings in the Settings screen
- Resources links on the About screen
- Section icons and a reusable header component for Settings
- Download retry analytics events
- Unit tests covering swipe action mappers, download update use cases, and downloads-with-metadata flow handling

### Changed
- Refined swipe threshold handling and swipe state behavior in the downloads UI
- Extracted shared YoutubeDL client and download work scheduler integrations
- Clarified app update strings and shared artifact rename task setup
- Updated Kotlin, Android Gradle Plugin, Gradle wrapper, Compose BOM, and Firebase BOM
- Reused the shared main dispatcher rule consistently across unit tests

### Fixed
- Swipe actions now trigger more reliably after crossing the threshold
- Swipe state now resets correctly when a download's seen state changes

## 1.7.0 - 2026-04-25
### Added
- Archived downloads screen with archive and unarchive actions
- Persisted archived state for downloads
- Unit tests covering archived downloads, restore scrolling, and hash formatting

### Changed
- Refined download and archived download navigation behavior
- Reworked update toast handling for more consistent UI feedback
- Suppressed completion notifications and toasts for archived downloads
- Updated theme surfaces and top app bar styling
- Updated Android Gradle Plugin, Compose BOM, and Navigation Compose
- Configured the Gradle daemon JVM toolchain

### Fixed
- Restored items now scroll back into view more reliably
- SHA-256 hash formatting is now locale-stable

## 1.6.1 - 2026-04-15
### Added
- Share action in download notifications
- Download metadata refresh flow, loading state in the downloads UI, and a dedicated background refresh worker

### Changed
- Download progress text format refinement
- More reliable thumbnail writes during metadata refresh handling
- Updated compileSdk and targetSdk to 37
- Updated Android Gradle Plugin, Gradle wrapper, and Firebase BOM

## 1.6.0 - 2026-04-10
### Added
- Active download notifications with thumbnail support and direct notification actions for stopping individual downloads
- Available update notification actions with cleanup handling and install flow integration
- Stop all downloads action in the downloads UI
- Retry failed downloads action with scroll-to-top behavior
- Delayed pending-delete worker flow for individual downloads
- Runtime initialization for YoutubeDL with reported output path support
- Unit tests covering notification action helpers and update scheduling

### Changed
- Refined downloads screen visuals, progress behavior, install update flow, and conversation item animations
- Reworked download and update scheduling around WorkManager periodic and trigger-based jobs
- Simplified foreground notification handling and reorganized notification action infrastructure
- Improved background work enqueueing, downloads filtering, and delete-check coordination
- Standardized youtube-dl output handling, runtime setup, and logging
- Updated Compose BOM, WorkManager, and ByteBuddy dependencies

### Fixed
- Notifications are now cleared correctly when retrying, marking downloads as seen, and cleaning up update state
- Download retries preserve foreground notification state more reliably
- YoutubeDL output file paths are now resolved more reliably without fragile stdout parsing

## 1.5.4 - 2026-03-24
### Added
- Pending delete flow for downloads with swipe dismiss and undo support
- Restore animations for downloads brought back from pending delete
- Bulk delete handling that distinguishes between visible and hidden selected downloads
- Bulk seen and unseen actions for selected downloads
- Mark unseen action on the Download screen
- Unit tests covering repositories, mappers, and utility helpers

### Changed
- Refined downloads list scroll behavior and item click handling
- Completed downloads are marked seen when their Download screen page becomes visible
- Unified download update use cases to support batch operations
- Extracted and refined pending delete snackbar handling
- Aligned data layer naming and set explicit surface colors on Material `Surface` components
- Updated Kotlin, Mockito, and Firebase BOM versions
- Cleaned up README formatting

### Fixed
- Corrected index bounds checks when resolving the next selected download
- Reset swipe state correctly when exiting downloads selection mode

## 1.5.3 - 2026-03-14
### Added
- Download progress reporting for app update downloads
- Unit tests covering download, downloads, about, settings, and updates ViewModels

### Changed
- Update downloads now validate cached and completed APK sizes before reuse or install
- Refined download selection behavior and downloads list auto-scroll handling
- Optimized Downloads and Download ViewModel flow collection and dispatching
- Made loading and error state alignment configurable
- Updated Core KTX, DataStore Preferences, Compose BOM, and Activity Compose versions

## 1.5.2 - 2026-03-05
### Changed
- Replaced singleton Room database setup with a Hilt-provided builder module
- Refined About screen navigation by switching from StateFlow state to SharedFlow events
- Simplified download paging and separated page-level UI data flow
- Extracted daily work schedule delay calculation into a reusable utility
- Updated Android Gradle Plugin to 9.1.0

## 1.5.1 - 2026-03-01
### Changed
- Refined navigation handling on the About screen
- Build number can now be long-pressed to copy the version name
- Tapping the build number opens the app’s system info screen
- Updated Compose, Coil, Firebase, Dagger, and Android Gradle Plugin versions
- Disabled automatic app backup handling

## 1.5.0 - 2026-02-13
### Added
- Search support on the Downloads screen
- Empty state when no downloads match the search query

### Changed
- Improved search interaction and input handling on the Downloads screen
- Refined selection behavior when multi-select mode is active
- Updated Compose and Activity Compose dependency versions

### Fixed
- Prevented issues caused by excessively long search queries

## 1.4.3 - 2026-01-27
### Fixed
- Restored the available update banner on the downloads screen

## 1.4.2 - 2026-01-27
### Changed
- Standardized spacing, icon sizes, and settings item layout
- Refined settings interaction feedback and shape usage
- Refactored internal mappers, workers, and apply-related logic
- Upgraded build tooling, Gradle, AGP, Hilt, and NDK configuration
- Updated README metadata and supported sites info

## 1.4.1 - 2026-01-17
### Changed
- Updated Hilt, Compose, and Firebase dependency versions
- Download pager refactored to remove direct ViewModel coupling
- Download selection and pager position now driven explicitly by download ID
- Download screen pager logic simplified and made more predictable

### Fixed
- Pager state desynchronization when switching between downloads

## 1.4.0 - 2026-01-14
### Added
- Multi-select support for downloads
- Select all action in the downloads overflow menu
- Bulk delete actions with undo for selected downloads
- Completed download timestamps shown in the download UI
- Detailed completed-at metadata on the Download screen
- Automatic duplicate download deletion setting
- Deduplication of downloads using metadata (source and video ID)
- SHA256 hash support for downloaded videos
- Background workers for bulk deletion and duplicate cleanup

### Changed
- Download deletion moved fully to WorkManager with expedited handling
- Download side effects refactored out of ViewModels into explicit events
- Download screen layout, spacing, and dimens usage standardized
- Duplicate handling centralized around metadata and hash-based workers
- Download database schema updated to support deduplication and hashing
- Download list scrolling behavior refined and made more predictable
- Reduced download action overlay scrim opacity to improve thumbnail visibility

### Fixed
- Audio download failures no longer abort video downloads
- Auto-scroll behaviour fixed when new items are queued
- Missing or partial video metadata handled safely without crashes
- Download item interaction correctly disabled during delete undo snackbars
- Snackbar and delete state no longer restored incorrectly after navigation
- Long status text now ellipsized to prevent layout overflow
- Correct pluralization in delete downloads foreground notifications

## 1.3.5 - 2026-01-03
### Fixed
- Download failures caused by excessively long filenames from certain sources
- Safer filename truncation based on UTF-8 byte size to avoid filesystem limits
- Preserved filename placeholder templates while ensuring generated filenames remain valid

## 1.3.4 - 2025-12-23
### Changed
- Updated Kotlin to KSP versions
- Improved downloaded video and audio filename templates
- Improved thumbnail naming and resolution handling

### Fixed
- Simplified and clarified save location toast messages

## 1.3.3 - 2025-12-21
### Fixed
- Primary download action icon now uses a white tint

## 1.3.2 - 2025-12-20
### Added
- Seen state tracking for completed downloads
- Visual indicator for unseen completed downloads in the list

### Changed
- Completed downloads now visually stand out until opened
- Download title styling updated for unseen completed items
- Wifi-only policy logic refactored into a combined use case package

### Fixed
- App update and dependency update toasts are now shown only when the app is in the foreground
- “Already up to date” feedback is now shown when no update is available

## 1.3.1 - 2025-12-18
### Added
- Thumbnails are now shown on downloads

### Changed
- Improved how media metadata and authorship information is written to downloaded files
- Updated Compose and Activity Compose libraries
- Small wording and branding text refinements

### Fixed
- Reduced metadata conflicts when processing and merging media files

## 1.3.0 - 2025-12-15
### Added
- Multi-download pager on the Download screen, allowing navigation between multiple downloads
- Ability to view and navigate between multiple downloads within a single Download screen
- Per-download media selection with independent video and audio state during a session

### Changed
- Download screen reworked to use an ordered, id-driven pager model
- Download ViewModel refactored to aggregate per-download state and metadata
- Layout, spacing, and elevation standardized across download-related screens
- Extracted reusable Copyright component for About screen
- Updated Android Gradle Plugin to 8.13.2
- Updated Firebase BoM to 34.7.0

### Fixed
- Delete and retry behavior in the download pager
- Selected media now persists per download within the current session
- Snackbar undo now scrolls to the top when restoring the first item in the list
- Improved padding consistency across download lists and pager pages
- Improved contrast by setting `onPrimary` color to white in both light and dark themes

## 1.2.3 - 2025-11-24
### Changed
- Dependency update flow improved with clearer toast messages.
- Updated dependencies and refined JNI debug symbol handling.

### Fixed
- Package replace now properly resets update state and requeues pending downloads.

## 1.2.2 - 2025-11-17
### Added
- Media duration now shown in the download screen
- Media file extensions are now stored for both video and audio
- Duration is now saved as part of download metadata
- New, cleaner split between metadata and progress in the download UI

### Changed
- Download UI structure reorganized for better clarity
- Download screen updated to use the new UI layout
- Extension pill component renamed and polished
- Database schema upgraded with a new migration

### Fixed
- Audio file extensions now detected correctly
- Device-to-device transfer fully enabled for app data

## 1.2.1 - 2025-11-12
### Changed
- Updated README and in-app intro text
- Refined phrasing in intro copy
- Renamed downloads SQL view and added timestamp sorting
- Migrated database to version 3
- Replaced custom Action Icon Button with standard Icon Button in Download screen.
- Updated Android Gradle Plugin to 8.13.1

### Fixed
- Update-check workers now record timestamps only after completion
- Removed early timestamp writes during worker execution

## 1.2.0 - 2025-11-11
### Added
- Split video and audio files into their own database tables
- Weighted progress display for combined video/audio downloads
- Media switcher on the Download screen to toggle between video and audio actions
- File extension pill directly on each download
- One-time “Check now” button for dependency updates
- New toast messages for dependency checks (“Done” / “Already up to date”)
- Mock data seeding for debug builds

### Changed
- Refactor of Download Worker with clearer phase handling and more accurate progress
- Simplified database and repository layers around media handling
- Refined update worker scheduling and dependency update flow
- Smarter handling of duplicate file saves with clearer messages
- Polished download and update screen visuals

### Fixed
- Duplicate file save toast now shows the file name and path instead of “file exists” message
- Safer directory creation and file path management during downloads
- More consistent behavior between audio and video downloads during play, save, and share actions

## 1.1.2 - 2025-11-05
### Added
- “Last checked” timestamps for app and dependency updates
- Saved and restored last update check times automatically

### Changed
- Refined updates screen layout and spacing
- Standardized icon sizes and switch styling
- Improved clarity between update info and settings

### Fixed
- More reliable update checks with retry and backoff handling
- Cleaner network handling and logging in update workers

## 1.1.1 - 2025-11-02
### Added
- Foreground download notifications now show progress and estimated time left
- Content descriptions and error messages localized
- Consistent app spacing, sizes and elevations

### Changed
- Simplified download screen layout and button behavior
- Updated Firebase components
- Polished overall design and spacing in downloads, download and settings

## 1.1.0 - 2025-10-29
### Added
- Updates screen with auto update options and manual check
- Update notification now launches installer when tapped

### Changed
- Updated Kotlin and KSP
- General UI and layout improvements
- Improved overall performance and stability

## 1.0.1 - 2025-10-27
### Added
- Notification for available updates
- New “Updates” notification channel and group for update alerts

### Changed
- Active downloads channel renamed to “Active tasks”

## 0.1.0 - 2025-10-25
### Added
- Initial release.
