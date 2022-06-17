package com.example.the_memory_game

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.the_memory_game.models.BoardSize
import com.example.the_memory_game.models.MemoryGame
import com.example.the_memory_game.models.UserImageList
import com.example.the_memory_game.utils.EXTRA_BOARD_SIZE
import com.example.the_memory_game.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG ="MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }
    private val db = Firebase.firestore
    private var gameName : String?=null
    private var customGameImages: List<String>? = null
    private lateinit var clRoot: CoordinatorLayout
    private lateinit var memoryGame : MemoryGame
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var adapter: MemoryBoardAdapter

    private var boardSize: BoardSize = BoardSize.EASY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

//        val intent = Intent(this,CreateActivity::class.java)
//        intent.putExtra(EXTRA_BOARD_SIZE,BoardSize.MEDIUM)
//        startActivity(intent)
          setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                //setup game again
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your current Game?", null, View.OnClickListener {
                        setupBoard()
                    })
                } else {
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
            R.id.mi_download -> {
                showdownloaddialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode== CREATE_REQUEST_CODE && resultCode==Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName==null){
                Log.e(TAG,"Got null custom game from createActivtiy")
                return
            }else{
                downloadGame(customGameName)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showdownloaddialog() {
      val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("fetch memory game",boardDownloadView,View.OnClickListener {
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }
    private fun downloadGame(customgameName: String) {
      db.collection("games").document(customgameName).get().addOnSuccessListener { document->
        val userImageList =  document.toObject(UserImageList::class.java)
          if(userImageList?.images == null){
              Log.e(TAG,"Invalid custom game data from Firestore")
              Snackbar.make(clRoot,"Sorry, couldn't find any game such as , '$customgameName'",Snackbar.LENGTH_LONG).show()
              return@addOnSuccessListener
          }
           val numCards = userImageList.images.size*2
            boardSize = BoardSize.getByValue(numCards)
          customGameImages = userImageList.images
          for(imageURL in userImageList.images){
              Picasso.get().load(imageURL).fetch()
          }
          Snackbar.make(clRoot,"You are now playing '$customgameName'",Snackbar.LENGTH_LONG).show()
          gameName= customgameName
          setupBoard()
      }.addOnFailureListener{ exception ->
          Log.e(TAG,"exception when retrieveing game",exception)
      }
    }

    private fun showCreationDialog() {
        val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board",boardSizeView, View.OnClickListener {
           val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
         // navigate to the new screen/activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
       val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose New size",boardSizeView, View.OnClickListener {
             boardSize = when(radioGroupSize.checkedRadioButtonId){
                    R.id.rbEasy -> BoardSize.EASY
                 R.id.rbMedium -> BoardSize.MEDIUM
                 else -> BoardSize.HARD
             }
            gameName == null
            customGameImages == null
            setupBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this,)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("OK"){ _,_ ->
            positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupBoard(){
        supportActionBar?.title = gameName?: getString(R.string.app_name)
        when(boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy:4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium:6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard:6 x 6"
                tvNumPairs.text = "Pairs: 0 / 12"
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize,customGameImages)
        adapter =   MemoryBoardAdapter(this,boardSize,memoryGame.cards,object:MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager  = GridLayoutManager(this,boardSize.getWidth())
    }
    private fun updateGameWithFlip(position: Int) {
        // Error handling
        if (memoryGame.haveWonGame()) {
            //alert the user
                Snackbar.make(clRoot, "You already won!",Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceup(position)){
            //alert the user
            Snackbar.make(clRoot, "Invalid Move!",Snackbar.LENGTH_SHORT).show()
            return
        }
        //fliping the card
  if(memoryGame.FlipCard(position)){
      Log.i(TAG,"Found a match! Num pairs found: ${memoryGame.numPairsFound}")
      val color = ArgbEvaluator().evaluate(
          memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
      ContextCompat.getColor(this,R.color.color_progress_none),
              ContextCompat.getColor(this,R.color.color_progress_full)
      ) as Int
      tvNumPairs.setTextColor(color)
      tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
      if(memoryGame.haveWonGame()){
          Snackbar.make(clRoot,"Youn won! congratulations",Snackbar.LENGTH_LONG).show()
          CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.RED,Color.GREEN,Color.MAGENTA)).oneShot()
      }
  }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}