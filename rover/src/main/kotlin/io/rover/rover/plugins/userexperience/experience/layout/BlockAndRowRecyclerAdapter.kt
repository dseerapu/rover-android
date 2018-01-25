package io.rover.rover.plugins.userexperience.experience.layout

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.barcode.BarcodeBlockView
import io.rover.rover.plugins.userexperience.experience.blocks.button.ButtonBlockView
import io.rover.rover.plugins.userexperience.experience.blocks.image.ImageBlockView
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableView
import io.rover.rover.plugins.userexperience.experience.blocks.rectangle.RectangleBlockView
import io.rover.rover.plugins.userexperience.experience.layout.row.RowView
import io.rover.rover.plugins.userexperience.experience.blocks.text.TextBlockView
import io.rover.rover.plugins.userexperience.experience.blocks.web.WebBlockView

class BlockAndRowRecyclerAdapter(
    private val viewModelSequence: List<LayoutableViewModel>
) : RecyclerView.Adapter<BlockAndRowRecyclerAdapter.LayoutableBlockHolder>() {
    override fun getItemCount(): Int {
        return viewModelSequence.size
    }

    override fun onBindViewHolder(holder: LayoutableBlockHolder, position: Int) {
        holder.viewModel = viewModelSequence[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayoutableBlockHolder {
        val type = ViewType.values()[viewType]
        return LayoutableBlockHolder(
            viewFactory(parent, type)
        )
    }

    override fun getItemViewType(position: Int): Int {
        val viewModel = viewModelSequence[position]
        return viewModel.viewType.ordinal
    }

    private fun viewFactory(parent: ViewGroup, viewType: ViewType): LayoutableView<in LayoutableViewModel> {
        return when (viewType) {
            ViewType.Row -> RowView(parent.context)
            ViewType.Rectangle -> RectangleBlockView(parent.context)
            ViewType.Text -> TextBlockView(parent.context)
            ViewType.Image -> ImageBlockView(parent.context)
            ViewType.Button -> ButtonBlockView(parent.context)
            ViewType.WebView -> WebBlockView(parent.context)
            ViewType.Barcode -> BarcodeBlockView(parent.context)
        } as LayoutableView<LayoutableViewModel>
    }

    /**
     * This [RecyclerView.ViewHolder] wraps a [LayoutableViewModel].
     */
    class LayoutableBlockHolder(
        private val layoutableItemView: LayoutableView<in LayoutableViewModel>
    ) : RecyclerView.ViewHolder(
        layoutableItemView.view
    ) {
        var viewModel: LayoutableViewModel? = null
            set(value) {
                if (value != null) {
                    layoutableItemView.viewModel = value
                }
                field = value
            }
    }
}
