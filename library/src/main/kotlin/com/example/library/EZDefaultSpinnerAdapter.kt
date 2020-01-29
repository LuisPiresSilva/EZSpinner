package com.example.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class EZDefaultSpinnerAdapter<Type>(
        private val customCreateViewHolder: ((layoutInflater : LayoutInflater, parent: ViewGroup, viewType: Int) -> RecyclerView.ViewHolder),
        private val customBindViewHolder: ((adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>, holder: RecyclerView.ViewHolder, position: Int, item : Type) -> Unit)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    EZSpinnerInterface<Type> {

    private lateinit var spinner: EZSpinnerView<Type>
    private val spinnerItems: MutableList<Type> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return customCreateViewHolder.invoke(layoutInflater, parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        customBindViewHolder.invoke(this, holder, position, spinnerItems[position])

    }

    override fun setItems(itemList: List<Type>) {
        this.spinnerItems.clear()
        this.spinnerItems.addAll(itemList)
        notifyDataSetChanged()
    }

    override fun notifyItemSelected(index: Int) {
        spinner.notifyItemSelected(index, spinnerItems[index])
    }

    override fun getItemCount() = this.spinnerItems.size

    override fun setEZSpinnerView(spinner: EZSpinnerView<Type>) {
        this.spinner = spinner
    }


}