package io.rover.rover.plugins.userexperience.experience.blocks.rectangle

import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.CompositeBlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableViewModel

/**
 * View Model for a block that contains no content (other than its own border and
 * background).
 */
interface RectangleBlockViewModelInterface : CompositeBlockViewModelInterface, LayoutableViewModel, BlockViewModelInterface, BackgroundViewModelInterface, BorderViewModelInterface