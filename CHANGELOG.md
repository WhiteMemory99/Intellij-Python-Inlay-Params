# Changelog

## [Unreleased]
### Changed
- Hide the hints that are 1 character long
- Don't show hints in call expressions that take a single parameter (except for `*args`)

### Fixed
- Hints showing when placing a positional argument after keyword ones

## [0.1.2] - 2022-07-18
### Fixed
- `**kwargs` parameter being shown in certain situations
- Class hints based on their attributes were shown incorrectly
- Fix hints display for calls with unpacking

## [0.1.1] - 2022-07-17
### Fixed
- Wrong hints behavior with some classes, related to the `__init__` inheritance logic
- Wrong hint ordering when a positional argument is passed after keyword arguments
- Messed up parameter ordering when unpacking is in the call expression

## [0.1.0] - 2022-07-15
### Added
- Initial plugin release
