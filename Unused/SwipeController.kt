package com.segihovav.mylinks_android

import android.annotation.SuppressLint
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.contains
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal enum class ButtonsState {
    GONE, RIGHT_VISIBLE
}

internal class SwipeController(private val buttonsActions: SwipeControllerActions, private var myLinksList: List<MyLink>) : ItemTouchHelper.Callback() {
    private var swipeBack = false
    private var buttonShowedState = ButtonsState.GONE
    private lateinit var buttonInstance: RectF
    private var mainActivity: AppCompatActivity? = null
    private var linkTypes: ArrayList<MyLinkType> = ArrayList()
    private var linkTypeNames: ArrayList<String> = ArrayList()
    private var darkMode: Boolean = false

    fun setDarkMode(_darkMode: Boolean) {
        this.darkMode=_darkMode
    }

    fun setLinkTypeNames(_linkTypeNames: ArrayList<String>) {
        this.linkTypeNames=_linkTypeNames
    }

    fun setLinkTypes(_linkTypes: ArrayList<MyLinkType>) {
         this.linkTypes=_linkTypes
    }

    fun setMainActivity(_mainActivity: AppCompatActivity) {
        this.mainActivity=_mainActivity
    }

    fun setMyLinksList(newMyLinksList: List<MyLink>) {
        myLinksList = newMyLinksList
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(0, ItemTouchHelper.LEFT)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = buttonShowedState != ButtonsState.GONE

            return 0
        }

        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        var dX = dX
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (buttonShowedState != ButtonsState.GONE) {
                if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) dX = min(dX, -buttonWidth) // Controls how far to the left or right the item will stick to
                     super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            } else {
                setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            drawButtons(c, viewHolder)
        }
        if (buttonShowedState == ButtonsState.GONE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            if (swipeBack) {
                if (dX < -buttonWidth) {
                    buttonShowedState = ButtonsState.RIGHT_VISIBLE
                }

                if (buttonShowedState != ButtonsState.GONE) {
                    setTouchDownListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    setItemsClickable(recyclerView, false)
                }
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchDownListener(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                setTouchUpListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchUpListener(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                super@SwipeController.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive)
                recyclerView.setOnTouchListener { _, _ -> false }
                setItemsClickable(recyclerView, true)
                swipeBack = false

                if (buttonInstance.contains(event.x,event.y)) {
                    if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) {
                        mainActivity?.let { buttonsActions.onLeftClicked(myLinksList, viewHolder.adapterPosition, it, linkTypes,linkTypeNames) }
                    }
                }

                buttonShowedState = ButtonsState.GONE
            }
            true
        }
    }

    private fun setItemsClickable(recyclerView: RecyclerView, isClickable: Boolean) {
        for (i in 0 until recyclerView.childCount) {
            recyclerView.getChildAt(i).isClickable = isClickable
        }
    }

    private fun drawButtons(c: Canvas, viewHolder: RecyclerView.ViewHolder) {
        val buttonWidthWithoutPadding = buttonWidth - 5
        val corners = 16f
        val itemView = viewHolder.itemView
        val p = Paint()

        val editBitmap = BitmapFactory.decodeResource(viewHolder.itemView.resources, if (!darkMode) R.drawable.edit_black else R.drawable.edit_white)
        //val leftButton = RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + buttonWidth, itemView.bottom.toFloat())
        //c.drawBitmap(editBitmap, null, leftButton, p)

        val rightButton = RectF(itemView.right.toFloat() - buttonWidth, itemView.top.toFloat(), itemView.right.toFloat() , itemView.bottom.toFloat())
        c.drawBitmap(editBitmap, null, rightButton, p)

        // left button - Good
        /*val leftButton = RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + buttonWidth, itemView.bottom.toFloat())
        p.color = Color.RED
        c.drawRoundRect(leftButton, corners, corners, p)
        drawText("Edit", c, leftButton, p, 0)*/

        if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) {
            //buttonInstance = leftButton
            buttonInstance = rightButton
        }
    }

    /*private fun drawText(text: String, c: Canvas, button: RectF, p: Paint, y: Int) {
        val textSize = 30f
        p.color = Color.WHITE
        p.isAntiAlias = true
        p.textSize = textSize

        val textWidth = p.measureText(text)

        c.drawText(text, button.centerX() - textWidth / 2, button.centerY() + textSize / 2 + y, p)

        //val editBitmap = BitmapFactory.decodeResource(null, R.drawable.edit)
        //c.drawBitmap(editBitmap,0,0,p);
    }*/

    companion object {
        private const val buttonWidth = 200f
    }
}
