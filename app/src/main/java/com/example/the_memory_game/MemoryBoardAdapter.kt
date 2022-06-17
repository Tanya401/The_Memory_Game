    package com.example.the_memory_game

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.the_memory_game.models.BoardSize
import com.example.the_memory_game.models.MemoryCard
import com.squareup.picasso.Picasso
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: MainActivity,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
)
    : RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {
    companion object{
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    interface CardClickListener{
        fun onCardClicked(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / boardSize.getWidth() - (2* MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2* MARGIN_SIZE)
        val cardSidelength = min(cardHeight,cardWidth)
       val view = LayoutInflater.from(context).inflate(R.layout.memory_card,parent,false) // output would be the view which was created
       val layoutParams =  view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSidelength
        layoutParams.height = cardSidelength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.bind(position)
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagebutton  = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard = cards[position]
            if(memoryCard.isFaceUp){
                if(memoryCard.imageUrl!=null){
                    Picasso.get().load(memoryCard.imageUrl).placeholder(R.drawable.ic_image).into(imagebutton)
                }
                else{
                    imagebutton.setImageResource(memoryCard.identifier)
                }
            }else {
                imagebutton.setImageResource(R.drawable.plants)
            }
            imagebutton.alpha = if(memoryCard.isMatched) 0.4f else 0.1f
           val colorStateList =  if(memoryCard.isMatched) ContextCompat.getColorStateList(context, R.color.color_gray)  else null
            ViewCompat.setBackgroundTintList(imagebutton,colorStateList)
      imagebutton.setOnClickListener{
          Log.i(TAG,"clicked on position $position")
          cardClickListener.onCardClicked(position)
      }
        }
    }
}
