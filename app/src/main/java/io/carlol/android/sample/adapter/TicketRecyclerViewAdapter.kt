package io.carlol.android.sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.carlol.android.sample.R
import io.carlol.android.sample.modelview.TicketModelView
import kotlinx.android.synthetic.main.view_item_rv_ticket.view.*


class TicketRecyclerViewAdapter : RecyclerView.Adapter<TicketRecyclerViewAdapter.ItemListChooserViewHolder>() {

    private val dataSet: ArrayList<TicketModelView> = ArrayList()


    fun updateDataset(dataset: List<TicketModelView>) {
        dataSet.clear()
        dataSet.addAll(dataset)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListChooserViewHolder {
        return ItemListChooserViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.view_item_rv_ticket,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemListChooserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemCount() = dataSet.size

    private fun getItem(position: Int) = dataSet[position]


    /*
     * ViewHolder
     */

    class ItemListChooserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: TicketModelView) {
            itemView.apply {
//                itemTicketContainerView.setAnchor(line)

                itemTicketImageView.setImageResource(item.imgResId)
                itemTickeLabelView.text = item.label
            }
        }
    }

}