package io.rover.rover.experiences.ui.layout

import io.rover.rover.ModelFactories
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.assets.ImageOptimizationServiceInterface
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.InjectionContainer
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.container.Scope
import io.rover.rover.core.data.domain.HorizontalAlignment
import io.rover.rover.core.data.domain.Length
import io.rover.rover.core.data.domain.Position
import io.rover.rover.core.data.domain.RectangleBlock
import io.rover.rover.core.data.domain.UnitOfMeasure
import io.rover.rover.core.data.domain.VerticalAlignment
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Observable
import io.rover.rover.experiences.ExperiencesAssembler
import io.rover.rover.experiences.MeasurementService
import io.rover.rover.experiences.ui.blocks.rectangle.RectangleBlockViewModelInterface
import io.rover.rover.experiences.ui.layout.row.RowViewModel
import io.rover.rover.experiences.types.RectF
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.CompositeBlockViewModelInterface
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class RowViewModelSpec : Spek({
    describe("integration tests with real block view models") {
        val realObjectStack = InjectionContainer(
            listOf(
                ExperiencesAssembler(),
                // now I need to override certain objects in the experiences assembler with mock ones.
                object : Assembler {
                    override fun assemble(container: Container) {
                        container.register(Scope.Singleton, AssetService::class.java) { resolver ->
                            mock()
                        }

                        container.register(Scope.Singleton, ImageOptimizationServiceInterface::class.java) { resolver ->
                            mock()
                        }

                        container.register(
                            Scope.Singleton,
                            MeasurementService::class.java
                        ) { _: Resolver -> mock() }
                    }
                }
            )
        )

        given("an autoheight row with stacked blocks") {
            val rowViewModel = RowViewModel(
                ModelFactories
                    .emptyRow()
                    .copy(
                        autoHeight = true,
                        height = Length(UnitOfMeasure.Points, 0.0),
                        blocks = listOf(
                            ModelFactories.emptyRectangleBlock().copy(
                                position = Position.Stacked,
                                height = Length(UnitOfMeasure.Points, 20.0),
                                width = Length(UnitOfMeasure.Points, 40.0)
                            ),
                            ModelFactories.emptyRectangleBlock().copy(
                                position = Position.Stacked,
                                height = Length(UnitOfMeasure.Points, 70.0),
                                width = Length(UnitOfMeasure.Points, 40.0)
                            )
                        )
                    ),
                { block -> realObjectStack.resolve(CompositeBlockViewModelInterface::class.java, null, block)!!},
                mock()
            )

            on("frame()") {
                // row bounds' bottom is given as 0, because rows are always responsible
                // for either setting their own height or measuring their stacked auto-height.
                val frame = rowViewModel.frame(RectF(0f, 0f, 60f, 0f))

                it("expands its height to contain the blocks") {
                    frame.bottom.shouldEqual(90f)
                }
            }

            on("frame() when erroneously given 0 width") {
                val frame = rowViewModel.frame(RectF(0f, 0f, 0f, 0f))

                it("expands its height to contain the blocks") {
                    frame.bottom.shouldEqual(90f)
                }
            }

            on("render()") {
                val layout = rowViewModel.mapBlocksToRectDisplayList(
                    // bottom is given here as 90f because mapBlocksToRectDisplayList must be called
                    // with the fully measured dimensions as returned by frame().
                    RectF(0f, 0f, 60f, 90f)
                )

                it("lays out the blocks in vertical order with no clip") {
                    layout.first().shouldMatch(
                        RectF(0f, 0f, 60f, 20f),
                        RectangleBlockViewModelInterface::class.java,
                        null
                    )
                    layout[1].shouldMatch(
                        RectF(0f, 20f, 60f, 90f),
                        RectangleBlockViewModelInterface::class.java,
                        null
                    )
                }
            }

            on("render() when erroneously given 0 width") {
                val layout = rowViewModel.mapBlocksToRectDisplayList(
                    // bottom is given here as 90f because mapBlocksToRectDisplayList must be called
                    // with the fully measured dimensions as returned by frame().
                    RectF(0f, 0f, 0f, 90f)
                )

                it("lays out the blocks in vertical order with a complete clip") {
                    layout.first().shouldMatch(
                        RectF(0f, 0f, 0f, 20f),
                        RectangleBlockViewModelInterface::class.java,
                        RectF(0f, 0f, 0f, 0f)
                    )
                    layout[1].shouldMatch(
                        RectF(0f, 20f, 0f, 90f),
                        RectangleBlockViewModelInterface::class.java,
                        RectF(0f, 20f, 0f, 20f)
                    )
                }
            }
        }

        given("a non-autoheight row with a floating block that extends outside the top of the row") {
            val rowViewModel = RowViewModel(
                ModelFactories
                    .emptyRow()
                    .copy(
                        autoHeight = false,
                        height = Length(UnitOfMeasure.Points, 20.0),
                        blocks = listOf(
                            ModelFactories.emptyRectangleBlock().copy(
                                position = Position.Floating,
                                height = Length(UnitOfMeasure.Points, 10.0),
                                width = Length(UnitOfMeasure.Points, 40.0),
                                offsets = ModelFactories.zeroOffsets().copy(
                                    top = Length(UnitOfMeasure.Points, -5.0)
                                )
                            )
                        )
                    ),
                { block -> realObjectStack.resolve(CompositeBlockViewModelInterface::class.java, null, block)!!},
                mock()
            )

            on("frame()") {
                // row bounds' bottom is given as 0, because rows are always responsible
                // for either setting their own height or measuring their stacked auto-height.
                val frame = rowViewModel.frame(RectF(0f, 0f, 60f, 0f))
                it("sets its height as the given value") {
                    frame.bottom.shouldEqual(20f)
                }
            }

            on("render()") {
                val layout = rowViewModel.mapBlocksToRectDisplayList(
                    rowViewModel.frame(RectF(0f, 0f, 40f, 0f))
                )

                it("lays out the floating block with a clip to chop the top bit off") {
                    // the amount that should be clipped off the top is -5
                    layout.first().shouldMatch(
                        RectF(0f, -5f, 40f, 5f),
                        RectangleBlockViewModelInterface::class.java,
                        RectF(0f, 5f, 40f, 10f )
                    )
                }
            }
        }

        given("a non-autoheight row with a floating block that extends outside the bottom of the row") {
            val rowViewModel = RowViewModel(
                ModelFactories
                    .emptyRow()
                    .copy(
                        autoHeight = false,
                        height = Length(UnitOfMeasure.Points, 20.0),
                        blocks = listOf(
                            ModelFactories.emptyRectangleBlock().copy(
                                position = Position.Floating,
                                height = Length(UnitOfMeasure.Points, 10.0),
                                width = Length(UnitOfMeasure.Points, 40.0),
                                offsets = ModelFactories.zeroOffsets().copy(
                                    top = Length(UnitOfMeasure.Points, 15.0)
                                )
                            )
                        )
                    ),
                { block -> realObjectStack.resolve(CompositeBlockViewModelInterface::class.java, null, block)!!},
                mock()
            )

            on("frame()") {
                // row bounds' bottom is given as 0, because rows are always responsible
                // for either setting their own height or measuring their stacked auto-height.
                val frame = rowViewModel.frame(RectF(0f, 0f, 60f, 0f))
                it("sets its height as the given value") {
                    frame.bottom.shouldEqual(20f)
                }
            }

            on("render()") {
                val layout = rowViewModel.mapBlocksToRectDisplayList(
                    rowViewModel.frame(RectF(0f, 0f, 40f, 0f))
                )

                it("lays out the floating block with a clip to chop the bottom bit off") {
                    // the amount that should be clipped off the bottom is 5
                    layout.first().shouldMatch(
                        RectF(0f, 15f, 40f, 25f),
                        RectangleBlockViewModelInterface::class.java,
                        RectF(0f, 0f, 40f, 5f)
                    )
                }
            }
        }

        given("a non-auto-height row with a floating block") {
            val blockHeight = 10.0
            val blockWidth = 30.0
            val rowHeight = 20.0

            fun nonAutoHeightRowWithFloatingBlock(
                verticalAlignment: VerticalAlignment,
                horizontalAlignment: HorizontalAlignment
            ): RowViewModel {
                return RowViewModel(
                    ModelFactories
                        .emptyRow()
                        .copy(
                            autoHeight = false,
                            height = Length(UnitOfMeasure.Points, rowHeight),
                            blocks = listOf(
                                ModelFactories.emptyRectangleBlock().copy(
                                    position = Position.Floating,
                                    height = Length(UnitOfMeasure.Points, blockHeight),
                                    width = Length(UnitOfMeasure.Points, blockWidth),
                                    verticalAlignment = verticalAlignment,
                                    horizontalAlignment = horizontalAlignment
                                )
                            )
                        ),
                    { block -> realObjectStack.resolve(CompositeBlockViewModelInterface::class.java, null, block)!!},
                    mock()
                )
            }

            given("a non-autoheight row with a floating block is aligned to the bottom") {
                val rowViewModel = nonAutoHeightRowWithFloatingBlock(
                    VerticalAlignment.Bottom,
                    HorizontalAlignment.Left
                )

                on("render()") {
                    val layout = rowViewModel.mapBlocksToRectDisplayList(
                        rowViewModel.frame(RectF(0f, 0f, 40f, 0f))
                    )

                    it("lays out the block on the bottom") {
                        layout.first().shouldMatch(
                            RectF(0f, 20f - 10f , 30f, 20f),
                            RectangleBlockViewModelInterface::class.java,
                            null
                        )
                    }
                }
            }

            given("a non-autoheight row with a floating block is aligned to the middle (vertical)") {
                val rowViewModel = nonAutoHeightRowWithFloatingBlock(
                    VerticalAlignment.Middle,
                    HorizontalAlignment.Left
                )

                on("render()") {
                    val layout = rowViewModel.mapBlocksToRectDisplayList(
                        rowViewModel.frame(RectF(0f, 0f, 40f, 0f))
                    )

                    it("lays out the block on the bottom") {
                        layout.first().shouldMatch(
                            RectF(0f, 5f, 30f, 15f),
                            RectangleBlockViewModelInterface::class.java,
                            null
                        )
                    }
                }
            }

            given("a non-autoheight row with a floating block is aligned to the center (horizontal)") {
                val rowViewModel = nonAutoHeightRowWithFloatingBlock(
                    VerticalAlignment.Top,
                    HorizontalAlignment.Center
                )

                on("render()") {
                    val layout = rowViewModel.mapBlocksToRectDisplayList(
                        rowViewModel.frame(RectF(0f, 0f, 40f, 0f))
                    )

                    it("lays out the block on the bottom") {
                        layout.first().shouldMatch(
                            RectF(5f, 0f, 35f, 10f),
                            RectangleBlockViewModelInterface::class.java,
                            null
                        )
                    }
                }
            }

            given("a non-autoheight row with a floating block that is aligned to the right") {
                val rowViewModel = nonAutoHeightRowWithFloatingBlock(
                    VerticalAlignment.Top,
                    HorizontalAlignment.Right
                )

                on("render()") {
                    val layout = rowViewModel.mapBlocksToRectDisplayList(
                        rowViewModel.frame(RectF(0f, 0f, 40f, 0f))
                    )

                    it("lays out the block on the bottom") {
                        layout.first().shouldMatch(
                            RectF(10f, 0f, 40f, 10f),
                            RectangleBlockViewModelInterface::class.java,
                            null
                        )
                    }
                }
            }
        }
    }

})
