package com.cnadia.anonymoussns

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.card_comment.view.*

class DetailActivity : AppCompatActivity() {
    val commentList = mutableListOf<Comment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val postId = intent.getStringExtra("postId")

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("mode", "comment")
            intent.putExtra("postId", postId)
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MyAdapter()

        FirebaseDatabase.getInstance().getReference("/posts/$postId")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot?.let {
                        val post = it.getValue(Post::class.java)
                        post?.let {
                            Picasso.get().load(it.bgUri)
                                .fit()
                                .centerCrop()
                                .into(backgroundImage)
                            contentsText.text = post.message
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        FirebaseDatabase.getInstance().getReference("/comments/$postId")
            .addChildEventListener(object: ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let { snapshot ->
                        val comment = snapshot.getValue(Comment::class.java)
                        comment?.let {
                            val prevIndex = commentList.map { it.commentId }.indexOf(previousChildName)
                            commentList.add(prevIndex+1, comment)
                            recyclerView.adapter?.notifyItemInserted(prevIndex+1)
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let {
                        val comment = it.getValue(Comment::class.java)
                        comment?.let { comment ->
                            val existIndex = commentList.map { it.commentId }.indexOf(comment.commentId)
                            commentList.removeAt(existIndex)

                            val prevIndex = commentList.map { it.commentId }.indexOf(previousChildName)
                            commentList.add(prevIndex+1, comment)
                            recyclerView.adapter?.notifyItemInserted(prevIndex+1)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    snapshot?.let {
                        val comment = it.getValue(Comment::class.java)
                        comment?.let { comment ->
                            val existIndex = commentList.map { it.commentId }.indexOf(comment.commentId)
                            commentList.removeAt(existIndex)
                            recyclerView.adapter?.notifyItemRemoved(existIndex)
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let {
                        val comment = it.getValue(Comment::class.java)
                        comment?.let { comment ->
                            val prevIndex = commentList.map { it.commentId }.indexOf(comment.commentId)
                            commentList[prevIndex+1] = comment
                            recyclerView.adapter?.notifyItemChanged(prevIndex+1)

                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    error?.toException()?.printStackTrace()
                }
            })
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val backgroundImge = itemView.backgroundImge
        val commentText = itemView.commentText
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(LayoutInflater
                            .from(this@DetailActivity)
                            .inflate(R.layout.card_comment, parent, false))
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val comment = commentList[position]

            comment?.let {
                Picasso.get()
                    .load(Uri.parse(it.bgUri))
//                    .fit()
//                    .centerCrop()
                    .into(holder.backgroundImge)
                holder.commentText.text = it.message
            }
        }

        override fun getItemCount(): Int {
            return commentList.size
        }
    }
}