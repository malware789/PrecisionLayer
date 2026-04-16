package com.example.precisionlayertesting.features.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.data.models.bug.Module
import com.example.precisionlayertesting.databinding.ItemModuleBinding

class ModuleAdapter(private var items: List<Module>) :
    RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {
    
    var onModuleClick: ((Module) -> Unit)? = null

    class ModuleViewHolder(val binding: ItemModuleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding

        // ── Card visuals (Premium defaults) ─────────────────────────────
        val cardBg = "#F5F7FF"
        val iconBg = "#002E88"
        b.root.setCardBackgroundColor(Color.parseColor(cardBg))
        b.iconContainer.setCardBackgroundColor(Color.parseColor(iconBg))

        // ── Text fields ──────────────────────────────────────────────────
        b.tvModuleName.text = item.name
        b.tvModuleDesc.text = "${item.description}" // Placeholder for description if not in DB

        // ── Status badge ─────────────────────────────────────────────────
        b.tvStatus.text = "ACTIVE"
        val badgeBg = b.tvStatus.background.mutate() as GradientDrawable
        badgeBg.setColor(Color.parseColor("#1A56DB"))
        b.tvStatus.background = badgeBg

        // ── Visibility fixes ─────────────────────────────────────────────
        b.ivAvatar1.visibility = View.VISIBLE
        b.ivAvatar2.visibility = View.VISIBLE
        b.ivAvatar3.visibility = View.GONE
        b.tvAnalyze.visibility = View.GONE // Hide until analyze logic ready

        // ── Click Navigation ──────────────────────────────────────────────
        b.root.setOnClickListener { onModuleClick?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Module>) {
        items = newItems
        notifyDataSetChanged()
    }
}
