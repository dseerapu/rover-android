package io.rover.rover.experiences

import android.os.Parcelable
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.assets.ImageOptimizationServiceInterface
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.container.Scope
import io.rover.rover.core.data.domain.*
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.logging.log
import io.rover.rover.experiences.ui.ExperienceViewModel
import io.rover.rover.experiences.ui.ExperienceViewModelInterface
import io.rover.rover.notifications.NotificationsRepository
import io.rover.rover.experiences.ui.blocks.barcode.BarcodeBlockViewModel
import io.rover.rover.experiences.ui.blocks.barcode.BarcodeViewModel
import io.rover.rover.experiences.ui.blocks.barcode.BarcodeViewModelInterface
import io.rover.rover.experiences.ui.blocks.button.*
import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModel
import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.border.BorderViewModel
import io.rover.rover.experiences.ui.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.*
import io.rover.rover.experiences.ui.blocks.concerns.text.AndroidRichTextToSpannedTransformer
import io.rover.rover.experiences.ui.blocks.concerns.text.RichTextToSpannedTransformer
import io.rover.rover.experiences.ui.blocks.concerns.text.TextViewModel
import io.rover.rover.experiences.ui.blocks.concerns.text.TextViewModelInterface
import io.rover.rover.experiences.ui.blocks.image.ImageBlockViewModel
import io.rover.rover.experiences.ui.blocks.image.ImageViewModel
import io.rover.rover.experiences.ui.blocks.image.ImageViewModelInterface
import io.rover.rover.experiences.ui.blocks.rectangle.RectangleBlockViewModel
import io.rover.rover.experiences.ui.blocks.text.TextBlockViewModel
import io.rover.rover.experiences.ui.blocks.web.WebViewBlockViewModel
import io.rover.rover.experiences.ui.blocks.web.WebViewModel
import io.rover.rover.experiences.ui.blocks.web.WebViewModelInterface
import io.rover.rover.experiences.ui.layout.row.RowViewModel
import io.rover.rover.experiences.ui.layout.row.RowViewModelInterface
import io.rover.rover.experiences.ui.layout.screen.ScreenViewModel
import io.rover.rover.experiences.ui.layout.screen.ScreenViewModelInterface
import io.rover.rover.experiences.ui.navigation.ExperienceNavigationViewModel
import io.rover.rover.experiences.ui.navigation.ExperienceNavigationViewModelInterface
import io.rover.rover.experiences.ui.toolbar.ExperienceToolbarViewModel
import io.rover.rover.experiences.ui.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.experiences.ui.toolbar.ToolbarConfiguration
import kotlin.reflect.KClass

/**
 * This is the Rover User Experience plugin.  It contains the entire Rover Experiences system.
 *
 * To use it to your project, add [ExperiencesAssembler] to your [Rover.initialize] invocation.
 */
class ExperiencesAssembler(
    private val applicationContext: android.content.Context
): Assembler {
    override fun assemble(container: Container) {
        container.register(
            Scope.Singleton,
            RichTextToSpannedTransformer::class.java
        ) { _ ->
            AndroidRichTextToSpannedTransformer()
        }

        container.register(
            Scope.Singleton,
            MeasurementService::class.java
        ) { resolver ->
            // perhaps some day Android might do per-display DisplayMetrics (which seems necessary
            // to support displays with diverse densities), in which case, this will need to change.
            AndroidMeasurementService(
                applicationContext.resources.displayMetrics,
                resolver.resolveSingletonOrFail(RichTextToSpannedTransformer::class.java)
            )
        }

        container.register(
            Scope.Transient,
            ExperienceToolbarViewModelInterface::class.java
        ) { _, toolbarConfiguration: ToolbarConfiguration ->
            ExperienceToolbarViewModel(toolbarConfiguration)
        }

        container.register(
            Scope.Transient,
            ExperienceNavigationViewModelInterface::class.java
        ) { resolver: Resolver, experience: Experience, icicle: Parcelable? ->
            ExperienceNavigationViewModel(
                experience,
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                { screen -> resolver.resolve(ScreenViewModelInterface::class.java, null, screen)!! },
                { toolbarConfiguration -> resolver.resolve(ExperienceToolbarViewModelInterface::class.java, null, toolbarConfiguration)!! },
                icicle
            )
        }

        container.register(
            Scope.Transient,
            BackgroundViewModelInterface::class.java
        ) { resolver: Resolver, background: Background ->
            BackgroundViewModel(
                background,
                resolver.resolveSingletonOrFail(AssetService::class.java),
                resolver.resolveSingletonOrFail(ImageOptimizationServiceInterface::class.java)
            )
        }

        container.register(
            Scope.Transient,
            ScreenViewModelInterface::class.java
        ) { resolver: Resolver, screen: Screen ->
            ScreenViewModel(
                screen,
                resolver.resolve(BackgroundViewModelInterface::class.java, null, screen)!!,
                { row -> resolver.resolve(RowViewModelInterface::class.java, null, row)!! }
            )
        }

        container.register(
            Scope.Transient,
            RowViewModelInterface::class.java
        ) { resolver: Resolver, row: Row ->
            RowViewModel(
                row,
                { block -> resolver.resolve(CompositeBlockViewModelInterface::class.java, null, block)!! },
                resolver.resolve(BackgroundViewModelInterface::class.java, null, row)!!
            )
        }

        container.register(
            Scope.Transient,
            ExperienceViewModelInterface::class.java
        ) { resolver: Resolver, experienceId: String, icicle: Parcelable? ->
            ExperienceViewModel(
                experienceId,
                resolver.resolveSingletonOrFail(GraphQlApiServiceInterface::class.java),
                { experience: Experience, navigationIcicle: Parcelable? ->
                    resolver.resolve(ExperienceNavigationViewModelInterface::class.java, null, experience, navigationIcicle)!!
                },
                icicle
            )
        }

        // embedded/block type view models:

        // this registered factory is a bit different: it's polymorphic, in that it will
        // yield different instances depending on the parameters (ie., the type of the given block).

        // block mixins:
        container.register(
            Scope.Transient,
            BlockViewModelInterface::class.java
        ) { resolver, block: Block, deflectors: Set<LayoutPaddingDeflection>, measurable: Measurable? ->
            BlockViewModel(
                block,
                deflectors,
                measurable
            )
        }

        container.register(
            Scope.Transient,
            TextViewModelInterface::class.java
        ) { resolver, text: Text ->
            TextViewModel(
                text,
                resolver.resolveSingletonOrFail(MeasurementService::class.java),
                false, false
            )
        }

        container.register(
            Scope.Transient,
            TextViewModelInterface::class.java,
            "buttonState"
        ) { resolver, text: Text ->
            TextViewModel(
                text,
                resolver.resolveSingletonOrFail(MeasurementService::class.java),
                // for buttons, Text should have no wrapping behaviour and center vertically.
                true, true
            )
        }

        container.register(
            Scope.Transient,
            ImageViewModelInterface::class.java
        ) { resolver, imageBlock: ImageBlock ->
            ImageViewModel(
                imageBlock,
                resolver.resolveSingletonOrFail(AssetService::class.java),
                resolver.resolveSingletonOrFail(ImageOptimizationServiceInterface::class.java)
            )
        }

        container.register(
            Scope.Transient,
            ButtonViewModelInterface::class.java
        ) { resolver, block: ButtonBlock, blockViewModel: BlockViewModelInterface ->
            ButtonViewModel(
                block, blockViewModel
            ) { buttonState ->
                resolver.resolve(ButtonStateViewModelInterface::class.java, null, buttonState)!!
            }
        }

        val buttonStateFactory = { resolver: Resolver, buttonState: ButtonState ->
            ButtonStateViewModel(
                resolver.resolve(BorderViewModelInterface::class.java, null, buttonState)!!,
                resolver.resolve(BackgroundViewModelInterface::class.java, null, buttonState)!!,
                // I need to ask for single line/center vertically.
                resolver.resolve(TextViewModelInterface::class.java, "buttonState", buttonState)!!
            )
        }

        val fdsafsd = buttonStateFactory::class

        val invokeMethods = fdsafsd.java.methods.toList().filter { it.name  == "invoke" }

        log.v("CLASS TYPE FOR BUTTON STATE FACTORY IS:\n    ${invokeMethods.joinToString("\n    ")}")

        container.register(
            Scope.Transient,
            ButtonStateViewModelInterface::class.java,
            null,
            buttonStateFactory
        )

        container.register(
            Scope.Transient,
            BorderViewModelInterface::class.java
        ) { _, border: Border ->
           BorderViewModel(
               border
           )
        }

        container.register(
            Scope.Transient,
            BackgroundViewModelInterface::class.java
        ) { resolver, background: Background ->
            BackgroundViewModel(
                background,
                resolver.resolveSingletonOrFail(AssetService::class.java),
                resolver.resolveSingletonOrFail(ImageOptimizationServiceInterface::class.java)
            )
        }

        container.register(
            Scope.Transient,
            WebViewModelInterface::class.java
        ) { _, block: WebViewBlock ->
            WebViewModel(
                block
            )
        }

        container.register(
            Scope.Transient,
            BarcodeViewModelInterface::class.java
        ) { resolver, block: BarcodeBlock ->
            BarcodeViewModel(
                block,
                resolver.resolveSingletonOrFail(MeasurementService::class.java)
            )
        }


        // row blocks:
        container.register(
            Scope.Transient,
            CompositeBlockViewModelInterface::class.java
        ) { resolver: Resolver, block: Block ->

            // TODO: perhaps I should register each block as its own factory in the container, to
            // make overriding individual ones easier.

            // crap I have a problem.  Because I my root interface is BlockViewModelInterface,
            // which naturally contains the layout concerns to all blocks, and I also use
            // a "BlockViewModel" mixin that implements that interface, I have an ambiguity while
            // trying to look up that block
            when(block) {
                is RectangleBlock -> {
                    RectangleBlockViewModel(
                        resolver.resolve(BlockViewModelInterface::class.java, null, block, setOf<LayoutPaddingDeflection>(),null)!!,
                        resolver.resolve(BackgroundViewModelInterface::class.java, null, block)!!,
                        resolver.resolve(BorderViewModelInterface::class.java, null, block)!!
                    )
                }
                is TextBlock -> {
                    val borderViewModel = resolver.resolve(BorderViewModelInterface::class.java, null, block)!!
                    val textViewModel = resolver.resolve(TextViewModelInterface::class.java, null, block)!!
                    TextBlockViewModel(
                        resolver.resolve(BlockViewModelInterface::class.java, null, block, setOf(borderViewModel), textViewModel)!!,
                        textViewModel,
                        resolver.resolve(BackgroundViewModelInterface::class.java, null, block)!!,
                        borderViewModel
                    )
                }
                is ImageBlock -> {
                    val imageViewModel = resolver.resolve(ImageViewModelInterface::class.java, null, block)!!
                    val borderViewModel = resolver.resolve(BorderViewModelInterface::class.java, null, block)!!
                    ImageBlockViewModel(
                        resolver.resolve(BlockViewModelInterface::class.java, null, block, setOf(borderViewModel), imageViewModel)!!,
                        resolver.resolve(BackgroundViewModelInterface::class.java, null, block)!!,
                        imageViewModel,
                        borderViewModel
                    )
                }
                is ButtonBlock -> {

                    // buttons have no measurable content and also cannot contribute padding, so we
                    // pass empty values for both the layout deflections set and

                    val blockViewModel = resolver.resolve(BlockViewModelInterface::class.java, null, block, setOf<LayoutPaddingDeflection>(),null)!!

                    ButtonBlockViewModel(
                        blockViewModel,
                        resolver.resolve(
                            ButtonViewModelInterface::class.java, null, block, blockViewModel
                        )!!
                    )
                }
                is WebViewBlock -> {
                    WebViewBlockViewModel(
                        resolver.resolve(BlockViewModelInterface::class.java, null, block, setOf<LayoutPaddingDeflection>(),null)!!,
                        resolver.resolve(BackgroundViewModelInterface::class.java, null, block)!!,
                        resolver.resolve(BorderViewModelInterface::class.java, null, block)!!,
                        resolver.resolve(WebViewModelInterface::class.java, null, block)!!
                    )
                }
                is BarcodeBlock -> {
                    val barcodeViewModel = resolver.resolve(BarcodeViewModelInterface::class.java, null, block)!!
                    val borderViewModel = resolver.resolve(BorderViewModelInterface::class.java, null, block)!!

                    BarcodeBlockViewModel(
                        resolver.resolve(BlockViewModelInterface::class.java, null, block, setOf(borderViewModel), barcodeViewModel)!!,
                        barcodeViewModel,
                        resolver.resolve(BackgroundViewModelInterface::class.java, null, block)!!,
                        borderViewModel
                    )
                }
                else -> throw Exception(
                    "This Rover UI block type is not yet supported by the 2.0 SDK: ${block.javaClass.simpleName}."
                )
            }
        }


        // crap. run into another problem.  so I commonly need to pass a given object to several
        // places in the context of setting, up, say a block in an Experience.  Right now it will
        // just yield new copies to each one.  That might be OK for many of them, since most of the
        // view models are not stateful, but it is wasteful and may prove a problem in the event of
        // a more stateful view model.  I can't even readily cheat by holding a reference and
        // passing it multiple times, because I'm recursing into the DI structure by calling
        // .resolve() for the various dependencies (and that's there the saved dep would be needed).
        // So, I'd have to break DI entirely and just construct each's block entire graph directly
        // in the CompositeBlockViewModelInterface factory.  That is no good, because that prevents
        // customization by overriding the individual view model mixins in the DI framework.

        // Solutions, one of:

        // * don't worry about VM mixin duplication, and avoid stateful VMs, particularly for view model mixins

        // * implement multiton support in DI, ideally with some sort of context resolver-overlay
        // that will restrict the memoization behaviour to a given context.

        // * implement multiton support in DI, without context awareness.  current behaviour like
        // current.

        // * defer it, because it appears that avoiding VM mixin duplication is only an
        // optimization, because all of the mixins appear to be (and should be, anyway) stateless.

    }
}
