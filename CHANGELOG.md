# Changelog

## [Unreleased]

### Added

- Support 2024.2

## [0.3.5] - 2024-01-24

### Added

- Support 2024.1

## [0.3.4] - 2023-12-18

### Added

- Support 2023.3
- An option to display parameter hints when there is only one parameter defined [[#42](https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/pull/42)]

## [[0.3.3]] - 2023-08-16

### Added

- Support for 2023.2. Please, note that this PyCharm version and older have built-in parameter hints that can be turned off in the same section.

## [[0.3.2]] - 2023-01-20

### Added

- Support for 2023.1

### Fixed

- Incorrect `Literal` behavior with type hints
- `set[]` type hint were causing exceptions when declared explicitly [[#31](https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/pull/31)]

## [0.3.1] - 2022-10-02

### Fixed

- `TypedDict` subclasses being displayed as a regular dict in type hints
- Several bugs around `async` functions with type hints
- Other minor type hints corrections

## [0.3.0] - 2022-09-25

### Added

- Type hints now support `async` functions [[#15](https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/pull/15)]
- Made type hints clickable, `Ctrl+LMB` to open the object reference [[#17](https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/pull/17)]

### Changed

- Parameter hints rewritten - now more reliable, with complete syntax coverage, including chained calls support.

### Fixed

- Redundant parameter hints for names that start with "`__`" or 1 character long
- Display of unnecessary type hints [[#16](https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/pull/16)]

## [0.2.1] - 2022-09-23

### Added

- Support for 2022.3
- More Python syntax covered with parameter hints

### Changed

- Reduced the amount of parameter hints to make them more relevant and valuable, a new settings option included

### Fixed

- Star (*) and Slash (/) parameters were breaking the hints order

## [0.2.0] - 2022-07-29

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

[Unreleased]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.3.5...HEAD
[0.3.5]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.1.4...v0.2.0
[0.1.4]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/WhiteMemory99/Intellij-Python-Inlay-Params/commits/v0.1.0
