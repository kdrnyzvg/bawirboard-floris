package com.example.notely

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notely.databinding.ItemNoteBinding
import java.text.DateFormat
import java.util.Date

class NotesAdapter(
    private var items: List<NoteMeta>,
    private val onClick: (NoteMeta) -> Unit,
    private val onLongClick: (NoteMeta) -> Unit
) : RecyclerView.Adapter<NotesAdapter.VH>() {

    inner class VH(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.noteTitle.text = item.title
        holder.binding.noteSnippet.text = item.snippet
        holder.binding.noteDate.text =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(item.updatedAt))
        holder.binding.root.setOnClickListener { onClick(item) }
        holder.binding.root.setOnLongClickListener { onLongClick(item); true }
    }

    override fun getItemCount() = items.size

    @Suppress("NotifyDataSetChanged")
    fun submit(newItems: List<NoteMeta>) {
        items = newItems
        notifyDataSetChanged()
    }
}
