package com.example.organicstate.databinding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.organicstate.R
import com.google.android.material.card.MaterialCardView

class ItemRecipeMessageBinding private constructor(
    val root: FrameLayout,
    val cvUserMessage: MaterialCardView,
    val tvUserMessage: TextView,
    val tvUserTime: TextView,
    val cvAiMessage: MaterialCardView,
    val tvAiMessage: TextView,
    val tvAiTime: TextView,
    val progressTyping: ProgressBar
) {
    companion object {
        fun inflate(inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean): ItemRecipeMessageBinding {
            val view = inflater.inflate(R.layout.item_recipe_message, parent, attachToParent)
            return bind(view)
        }

        fun bind(view: View): ItemRecipeMessageBinding {
            val cvUserMessage = view.findViewById<MaterialCardView>(R.id.cvUserMessage)
            val tvUserMessage = view.findViewById<TextView>(R.id.tvUserMessage)
            val tvUserTime = view.findViewById<TextView>(R.id.tvUserTime)
            val cvAiMessage = view.findViewById<MaterialCardView>(R.id.cvAiMessage)
            val tvAiMessage = view.findViewById<TextView>(R.id.tvAiMessage)
            val tvAiTime = view.findViewById<TextView>(R.id.tvAiTime)
            val progressTyping = view.findViewById<ProgressBar>(R.id.progressTyping)

            return ItemRecipeMessageBinding(
                view as FrameLayout,
                cvUserMessage,
                tvUserMessage,
                tvUserTime,
                cvAiMessage,
                tvAiMessage,
                tvAiTime,
                progressTyping
            )
        }
    }
}