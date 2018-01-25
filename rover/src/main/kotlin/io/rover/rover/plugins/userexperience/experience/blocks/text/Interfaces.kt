package io.rover.rover.plugins.userexperience.experience.blocks.text

import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.TextViewModelInterface

/**
 * View Model for a block that contains rich text content (decorated with strong, italic, and
 * underline HTML tags).
 */
interface TextBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    TextViewModelInterface