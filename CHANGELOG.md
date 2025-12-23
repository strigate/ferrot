# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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