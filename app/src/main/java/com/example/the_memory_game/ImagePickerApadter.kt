package com.example.the_memory_game

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.the_memory_game.models.BoardSize
import kotlin.math.min

class ImagePickerApadter( private val context: CreateActivity,
                         private val imageUris: List<Uri>,
                         private val boardSize: BoardSize,
                         private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerApadter.ViewHolder>() {
    interface ImageClickListener{
        fun onPlaceHolderClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

         val view =  LayoutInflater.from(context).inflate(R.layout.card_image,parent,false)
       val cardWidth= parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardHeight,cardWidth)
       val layoutParams= view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardWidth
        layoutParams.height = cardHeight
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       // bind how do we display UI
        if(position <imageUris.size){
            holder.bind(imageUris[position])
        }
       else{
           holder.bind()
        }
    }

    override fun getItemCount() = boardSize.getNumPairs()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)
        fun bind(uri: Uri){
          ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }
  fun bind(){
        ivCustomImage.setOnClickListener{
            imageClickListener.onPlaceHolderClicked()
        }
    }
    }
}
