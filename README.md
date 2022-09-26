# Python Inlay Params

![Build](https://github.com/WhiteMemory99/intellij-python-inlay-params/workflows/Build/badge.svg)
![Version](https://img.shields.io/jetbrains/plugin/v/19538)
![Rating](https://img.shields.io/jetbrains/plugin/r/rating/19538)
![Downloads](https://img.shields.io/jetbrains/plugin/d/19538)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Quick Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> >   
  <kbd>Search for "Python Inlay Params"</kbd> > <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/WhiteMemory99/intellij-python-inlay-params/releases/latest) and
  install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Description

<!-- Plugin description -->
This plugin _(quite decently)_ implements inlay parameter hints for **Python**.  
In addition, you can enable type annotation hints for variables and function return types.

### Supported expressions

Pretty much any expression is supported:

* dataclass and class instantiation
* lambdas
* decorators
* function and method calls
* etc.

<!-- Plugin description end -->

**Look at the examples below to see how it works.**

## Settings

### Parameter hints

To disable them entirely, follow this path:

<kbd>Settings</kbd> -> <kbd>Editor</kbd> -> <kbd>Inlay Hints</kbd> -> <kbd>Parameter names</kbd> -> <kbd>Python</kbd>

But you can also switch specific parts in that menu: `Class hints`, `Function hints` to
enable/disable them individually.
Additionally, there's an option to avoid redundant hints more aggressively, called `Hide overlaps`, which is used to
hide a hint if a parameter name is completely overlapped by its argument.

### Type annotation hints (BETA)

To disable them entirely, follow this path:

<kbd>Settings</kbd> -> <kbd>Editor</kbd> -> <kbd>Inlay Hints</kbd> -> <kbd>Types</kbd> -> <kbd>Python</kbd>

There are two main options under this partition: `Type annotations` and `Function return types`.  
Look at the demo screenshot to see them in work _(last section)_.

## Demo Screenshot

![](.github/readme/demo_screenshot.png)
