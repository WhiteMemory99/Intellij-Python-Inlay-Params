# Changelog

## [Unreleased]
### Added
- Detail plugin settings that let you disable parameter hints selectively
- Introduce new inlay hints - optional type annotations for variables and functions return types [[#6](https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/pull/6)]

### Changed
- Drop support for build 212 and below due to changes in the plugin

## [0.1.4] - 2022-07-20
### Changed
- Lower minimal build requirement to 2021.2

### Fixed
- Hints not showing when there's one positional parameter with `**kwargs`

## [0.1.3] - 2022-07-19
### Changed
- Hide the hints that are 1 character long
- Don't show hints in call expressions that take a single parameter (except for `*args`)

### Fixed
- Hints showing when placing a positional argument after keyword ones
- Hints for classes that use `__new__`, like `datetime.datetime`
- Arguments with the same name as the parameter, but in a different case, were still displayed

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