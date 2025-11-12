# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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