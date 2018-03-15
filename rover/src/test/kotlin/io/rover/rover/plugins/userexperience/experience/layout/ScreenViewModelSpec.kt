package io.rover.rover.experiences.ui.layout

import io.rover.rover.ModelFactories
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.InjectionContainer
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.container.Scope
import io.rover.rover.core.data.domain.HorizontalAlignment
import io.rover.rover.core.data.domain.ID
import io.rover.rover.core.data.domain.Length
import io.rover.rover.core.data.domain.Offsets
import io.rover.rover.core.data.domain.UnitOfMeasure
import io.rover.rover.core.data.domain.VerticalAlignment
import io.rover.rover.experiences.ExperiencesAssembler
import io.rover.rover.experiences.MeasurementService
import io.rover.rover.experiences.ui.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.experiences.ui.blocks.rectangle.RectangleBlockViewModel
import io.rover.rover.experiences.ui.layout.row.RowViewModel
import io.rover.rover.experiences.ui.layout.screen.ScreenViewModel
import io.rover.rover.experiences.types.RectF
import io.rover.rover.experiences.ui.blocks.concerns.layout.CompositeBlockViewModelInterface
import io.rover.rover.experiences.ui.layout.row.RowViewModelInterface
import io.rover.rover.experiences.ui.navigation.ExperienceNavigationViewModelInterface
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class ScreenViewModelSpec : Spek({
    given("integration tests with real row view models") {

        val realObjectStack = InjectionContainer(
            listOf(
                ExperiencesAssembler(),
                // now I need to override certain objects in the experiences assembler with mock ones.
                object : Assembler {
                    override fun assemble(container: Container) {
                        container.register(
                            Scope.Singleton,
                            MeasurementService::class.java
                        ) { _: Resolver -> mock() }
                    }
                }
            )
        )

        given("a basic screen with one row with a rectangle block") {
            val screen = ModelFactories.emptyScreen().copy(
                rows = listOf(
                    ModelFactories.emptyRow().copy(
                        height = Length(UnitOfMeasure.Points, 10.0),
                        blocks = listOf(
                            ModelFactories.emptyRectangleBlock().copy(
                                height = Length(UnitOfMeasure.Points, 10.0)
                            )
                        )
                    )
                )
            )
            val screenViewModel = ScreenViewModel(
                screen,
                mock(),
                { row ->
                    realObjectStack.resolve(
                        RowViewModelInterface::class.java,
                        null,
                        row
                    )!!
                }
            )

            on("rendering") {
                val rendered = screenViewModel.render(
                    40f
                )

                it("puts the block directly on top of the row") {
                    // we have two
                    rendered.coordinatesAndViewModels[0].shouldMatch(
                        RectF(0f, 0f, 40f, 10f),
                        RowViewModel::class.java
                    )

                    rendered.coordinatesAndViewModels[1].shouldMatch(
                        RectF(0f, 0f, 40f, 10f),
                        RectangleBlockViewModel::class.java
                    )
                }
            }
        }

        given("a screen with two rows") {
            val screen = ModelFactories.emptyScreen().copy(
                rows = listOf(
                    ModelFactories.emptyRow().copy(
                        id = ID("1"),
                        height = Length(UnitOfMeasure.Points, 10.0),
                        blocks = listOf(
                            ModelFactories.emptyRectangleBlock().copy(
                                height = Length(UnitOfMeasure.Points, 10.0)
                            )
                        )
                    ),
                    ModelFactories.emptyRow().copy(
                        id = ID("2"),
                        height = Length(UnitOfMeasure.Points, 42.0)
                    )
                )
            )
            val screenViewModel = ScreenViewModel(screen, mock(), { row -> mock() })

            on("rendering") {
                val rendered = screenViewModel.render(
                    40f
                )

                it("puts the block directly on top of the row") {
                    // we have two
                    rendered.coordinatesAndViewModels[0].shouldMatch(
                        RectF(0f, 0f, 40f, 10f),
                        RowViewModel::class.java
                    )

                    rendered.coordinatesAndViewModels[1].shouldMatch(
                        RectF(0f, 0f, 40f, 10f),
                        RectangleBlockViewModel::class.java
                    )

                    rendered.coordinatesAndViewModels[2].shouldMatch(
                        // check that the rows are stacked properly: 0 + 10 + 42 = 52
                        RectF(0f, 10f, 40f, 52f),
                        RowViewModel::class.java
                    )
                }
            }
        }

        given("a screen with a row and a block with a vertical fill offset and a horizontal left offset") {
            val screen = ModelFactories.emptyScreen().copy(
                rows = listOf(
                    ModelFactories.emptyRow().copy(
                        id = ID("1"),
                        height = Length(UnitOfMeasure.Points, 100.0),
                        blocks = listOf(
                            ModelFactories.emptyRectangleBlock().copy(
                                height = Length(UnitOfMeasure.Points, 10.0),
                                width = Length(UnitOfMeasure.Points, 20.0),
                                horizontalAlignment = HorizontalAlignment.Left,
                                verticalAlignment = VerticalAlignment.Fill,
                                offsets = Offsets(
                                    bottom = Length(UnitOfMeasure.Points, 10.0),
                                    center = Length(UnitOfMeasure.Points, 30.0),
                                    left = Length(UnitOfMeasure.Points, 5.0),
                                    middle = Length(UnitOfMeasure.Points, 8.0),
                                    right = Length(UnitOfMeasure.Points, 10.0),
                                    top = Length(UnitOfMeasure.Points, 10.0)
                                )
                            )
                        )
                    ),
                    ModelFactories.emptyRow().copy(
                        id = ID("2"),
                        height = Length(UnitOfMeasure.Points, 42.0)
                    )
                )
            )
            val screenViewModel = ScreenViewModel(screen, mock(), { row -> mock() })

            on("rendering") {
                val rendered = screenViewModel.render(
                    40f
                )

                it("renders the block with the offsets") {
                    rendered.coordinatesAndViewModels[1].shouldMatch(
                        RectF(5f, 10f, 25f, 90f),
                        RectangleBlockViewModel::class.java
                    )
                }
            }
        }

        given("a screen with a row with a vertically centered block within") {
            val screen = ModelFactories.emptyScreen().copy(
                rows = listOf(
                    ModelFactories.emptyRow().copy(
                        height = Length(UnitOfMeasure.Points, 100.0),
                        blocks = listOf(
                            ModelFactories.emptyRectangleBlock().copy(
                                width = Length(UnitOfMeasure.Points, 10.0),
                                height = Length(UnitOfMeasure.Points, 20.0),
                                verticalAlignment = VerticalAlignment.Middle
                            )
                        )
                    )
                )
            )
            val screenViewModel = ScreenViewModel(screen, mock(), { row -> mock() })

            on("rendering") {
                val rendered = screenViewModel.render(
                    300f
                )

                it("centers the block inside the row") {
                    rendered.coordinatesAndViewModels[1].shouldMatch(
                        RectF(0f, 40f, 300f, 60f),
                        RectangleBlockViewModel::class.java
                    )
                }
            }
        }
    }
})

fun DisplayItem.shouldMatch(
    position: RectF,
    type: Class<out LayoutableViewModel>,
    clip: RectF? = null
) {
    this.position.shouldEqual(position)
    this.viewModel.shouldBeInstanceOf(type)
    this.clip.shouldEqual(clip)
}