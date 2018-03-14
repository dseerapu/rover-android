package io.rover.rover.experiences.ui.blocks.text

import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.CompositeBlockViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.experiences.ui.blocks.concerns.text.TextViewModelInterface

/**
 * View Model for a block that contains rich text content (decorated with strong, italic, and
 * underline HTML tags).
 */
interface TextBlockViewModelInterface :
    CompositeBlockViewModelInterface,
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    TextViewModelInterface