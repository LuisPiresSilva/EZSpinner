package com.example.library

interface EZSpinnerInterface <Type> {
    fun setEZSpinnerView(spinner: EZSpinnerView<Type>)
    fun notifyItemSelected(index: Int)
    fun setItems(itemList: List<Type>)
}