package com.example.library

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.RecyclerView

/** A lightweight dropdown spinner, fully customizable with arrow and animations. */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@SuppressLint("InflateParams")
class EZSpinnerView<Type> : FrameLayout, LifecycleObserver {

    private var spinnerBodyLayoutRef: Int = -1
    private var spinnerRecyclerRef: Int = -1

    private lateinit var spinnerBody: View
    private lateinit var spinnerWindow: PopupWindow
    private lateinit var spinnerRecyclerView: RecyclerView
    private lateinit var adapter: EZSpinnerInterface<Type>
    var isShowing: Boolean = false
        private set
    var selectedIndex: Int = -1
        private set
    var arrowAnimate: Boolean = true
    var arrowAnimationDuration: Long = 250L
    var arrowDrawable: View? = null

    @DrawableRes
    var arrowResource: Int = -1
        set(value) {
            field = value
        }

    @Px
    var spinnerPopupElevation: Int = context.dp2Px(4)
        set(value) {
            field = value
            updateSpinnerWindow()
        }
    var dismissWhenNotifiedItemSelected: Boolean = true

    var spinnerPopupAnimation: EZSpinnerAnimation = EZSpinnerAnimation.NORMAL
    @StyleRes
    var spinnerPopupAnimationStyle: Int = -1
    var spinnerPopupWidth: Int = -1
    var spinnerPopupHeight: Int = -1

    var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            field = value
            field?.lifecycle?.addObserver(this@EZSpinnerView)
        }

    fun init() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        this.spinnerBody = inflater.inflate(spinnerBodyLayoutRef, null)
        this.spinnerWindow = PopupWindow(this.spinnerBody,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        )
        this.setOnClickListener {
            showOrDismiss()
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        getAttrs(attributeSet)
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(
            context,
            attributeSet,
            defStyle
    ) {
        getAttrs(attributeSet, defStyle)
        init()
    }

    private fun getAttrs(attributeSet: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.EZSpinnerView)
        try {
            setTypeArray(typedArray)
        } finally {
            typedArray.recycle()
        }
    }

    private fun getAttrs(attributeSet: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
                attributeSet,
                R.styleable.EZSpinnerView,
                defStyleAttr,
                0
        )
        try {
            setTypeArray(typedArray)
        } finally {
            typedArray.recycle()
        }
    }

    private fun setTypeArray(a: TypedArray) {
        this.arrowResource = a.getResourceId(R.styleable.EZSpinnerView_spinner_rotating_view, -1)

        this.spinnerBodyLayoutRef = a.getResourceId(R.styleable.EZSpinnerView_popup_layout, this.spinnerBodyLayoutRef)

        this.spinnerRecyclerRef = a.getResourceId(R.styleable.EZSpinnerView_recycler_view, this.spinnerRecyclerRef)

        this.arrowAnimate = arrowResource != -1

        this.arrowAnimationDuration =
                a.getInteger(R.styleable.EZSpinnerView_spinner_arrow_animate_duration,
                        this.arrowAnimationDuration.toInt()).toLong()

        when (a.getInteger(R.styleable.EZSpinnerView_spinner_popup_animation,
                this.spinnerPopupAnimation.value)) {
            EZSpinnerAnimation.DROPDOWN.value -> this.spinnerPopupAnimation =
                EZSpinnerAnimation.DROPDOWN
            EZSpinnerAnimation.FADE.value -> this.spinnerPopupAnimation = EZSpinnerAnimation.FADE
            EZSpinnerAnimation.BOUNCE.value -> this.spinnerPopupAnimation = EZSpinnerAnimation.BOUNCE
        }
        this.spinnerPopupAnimationStyle =
                a.getResourceId(R.styleable.EZSpinnerView_spinner_popup_animation_style,
                        this.spinnerPopupAnimationStyle)
        this.spinnerPopupWidth =
                a.getDimensionPixelSize(R.styleable.EZSpinnerView_spinner_popup_width,
                        this.spinnerPopupWidth)
        this.spinnerPopupHeight =
                a.getDimensionPixelSize(R.styleable.EZSpinnerView_spinner_popup_height,
                        this.spinnerPopupHeight)
        this.spinnerPopupElevation =
                a.getDimensionPixelSize(R.styleable.EZSpinnerView_spinner_popup_elevation,
                        this.spinnerPopupElevation)
        val itemArray = a.getResourceId(R.styleable.EZSpinnerView_spinner_item_array, -1)
        if (itemArray != -1) {
            setItems(itemArray)
        }
        this.dismissWhenNotifiedItemSelected =
                a.getBoolean(R.styleable.EZSpinnerView_spinner_dismiss_notified_select,
                        this.dismissWhenNotifiedItemSelected)

    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        updateSpinnerWindow()
        arrowDrawable = findViewById(arrowResource)
        spinnerRecyclerView = spinnerBody.findViewById(spinnerRecyclerRef)
    }


    private fun updateSpinnerWindow() {
        post {
            this.spinnerWindow.apply {
                width = this@EZSpinnerView.width
                isOutsideTouchable = true
                setTouchInterceptor(object : OnTouchListener {
                    @SuppressLint("ClickableViewAccessibility")
                    override fun onTouch(view: View, event: MotionEvent): Boolean {
                        if (event.action == MotionEvent.ACTION_OUTSIDE) {
                            if(::outsideTouchCallback.isInitialized) {
                                outsideTouchCallback.invoke()
                            }
                            this@EZSpinnerView.setOnClickListener {}
                            showOrDismiss()
                            Handler().postDelayed({
                                this@EZSpinnerView.setOnClickListener { showOrDismiss() }
                            },500)
                            return true
                        }
                        return false
                    }
                })
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    elevation = spinnerPopupElevation.toFloat()
                }

            }

            if (this.spinnerPopupWidth != -1) {
                this.spinnerWindow.width = this.spinnerPopupWidth
            }
            if (this.spinnerPopupHeight != -1) {
                this.spinnerWindow.height = this.spinnerPopupHeight
            }
        }
    }


    private fun applyWindowAnimation() {
        if (this.spinnerPopupAnimationStyle == -1) {
            when (this.spinnerPopupAnimation) {
                EZSpinnerAnimation.DROPDOWN -> this.spinnerWindow.animationStyle = R.style.DropDown
                EZSpinnerAnimation.FADE -> this.spinnerWindow.animationStyle = R.style.Fade
                EZSpinnerAnimation.BOUNCE -> this.spinnerWindow.animationStyle = R.style.Elastic
                else -> Unit
            }
        } else {
            this.spinnerWindow.animationStyle = this.spinnerPopupAnimationStyle
        }
    }

    /** gets the spinner popup's recyclerView. */
    fun getSpinnerRecyclerView(): RecyclerView? = if(::spinnerRecyclerView.isInitialized) {this.spinnerRecyclerView} else null

    /** sets an item list for setting items of the adapter. */
    @Suppress("UNCHECKED_CAST")
    fun <Type> setItems(itemList: List<Type>) {
        if(::adapter.isInitialized) {
            val adapter = this.adapter as EZSpinnerInterface<Type>
            adapter.setItems(itemList)
        } else {
            throw Exception("You must set your adapter first")
        }
    }

    /**
     * sets a string array resource for setting items of the adapter.
     * This function only works for the [EZDefaultSpinnerAdapter].
     */
    fun setItems(@ArrayRes resource: Int) {
        if (this.adapter is EZDefaultSpinnerAdapter) {
            (this.adapter as EZDefaultSpinnerAdapter).setItems(
                    context.resources.getStringArray(resource).toList() as List<Type>)
        }
    }

    /** sets an adapter of the [EZSpinnerView]. */
    fun setSpinnerAdapter(ezSpinnerInterface: EZSpinnerInterface<Type>) {
        this.adapter = ezSpinnerInterface
        this.adapter.setEZSpinnerView(this)

        if (this.adapter is RecyclerView.Adapter<*>) {
            if(::spinnerRecyclerView.isInitialized) {
                this.spinnerRecyclerView.adapter = this.adapter as RecyclerView.Adapter<*>
            }
        }
    }

    /** gets an adapter of the [EZSpinnerView]. */
    @Suppress("UNCHECKED_CAST")
    fun <Type> getSpinnerAdapter(): EZSpinnerInterface<Type>? {
        return if(::adapter.isInitialized) {
            this.adapter as EZSpinnerInterface<Type>
        } else {
            null
        }

    }


    private lateinit var selectedItemCallback: (position: Int, item: Type) -> Unit
    /** sets a [OnSpinnerItemSelectedListener] to the popup using lambda. */
    @Suppress("UNCHECKED_CAST")
    fun setOnSpinnerItemSelectedListener(block: (position: Int, item: Type) -> Unit) {
        selectedItemCallback = block
    }

    private lateinit var outsideTouchCallback: () -> Unit
    /** sets a [OnSpinnerOutsideTouchListener] to the popup using lambda. */
    fun setOnPopupOutsideTouchListener(unit: ()  -> Unit) {
        outsideTouchCallback = unit
    }

    private lateinit var onShowCallback: () -> Unit
    /** sets a [OnShowListener] to the popup using lambda. */
    fun setOnShowListener(unit: ()  -> Unit) {
        onShowCallback = unit
    }

    private lateinit var onDismissCallback: () -> Unit
    /** sets a [OnDismissListener] to the popup using lambda. */
    fun setOnDismissListener(unit: ()  -> Unit) {
        onDismissCallback = unit
    }


    /** shows the spinner popup menu to the center. */
    @MainThread
    fun show() {
        if (!this.isShowing) {
            this.isShowing = true
            animateArrow(true)
            applyWindowAnimation()
            this.spinnerWindow.showAsDropDown(this)
            if(::onShowCallback.isInitialized) {
                this.onShowCallback.invoke()
            }
        }
    }

    /** dismiss the spinner popup menu. */
    @MainThread
    fun dismiss() {
        if (this.isShowing) {
            animateArrow(false)
            this.spinnerWindow.dismiss()
            this.isShowing = false
            if(::onDismissCallback.isInitialized) {
                this.onDismissCallback.invoke()
            }
        }
    }

    /**
     * If the popup is not showing, shows the spinner popup menu to the center.
     * If the popup is already showing, dismiss the spinner popup menu.
     */
    @MainThread
    fun showOrDismiss() {
        if (!this.isShowing) {
            show()
        } else {
            dismiss()
        }
    }

    /** select an item by index. */
    fun selectItemByIndex(index: Int) {
        this.adapter.notifyItemSelected(index)
    }

    /** notifies to [EZSpinnerView] of changed information from [EZSpinnerInterface]. */
    fun notifyItemSelected(index: Int, obj: Type) {
        this.selectedIndex = index

        if(::selectedItemCallback.isInitialized) {
            selectedItemCallback.invoke(index, obj)
        }

        if (this.dismissWhenNotifiedItemSelected) {
            dismiss()
        }
    }

    /** animates the arrow rotation. */
    private fun animateArrow(shouldRotateUp: Boolean) {
        if (this.arrowAnimate) {
            val end = if (shouldRotateUp) 180F else 0F
            ObjectAnimator.ofFloat(this.arrowDrawable, "rotation", arrowDrawable?.rotation
                    ?: 0F, end).apply {
                duration = this@EZSpinnerView.arrowAnimationDuration
                start()
            }
        }
    }

    /** dismiss automatically when lifecycle owner is destroyed. */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        dismiss()
    }

}
