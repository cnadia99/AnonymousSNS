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
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.card_post.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Minutes
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    val posts: MutableList<Post> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "글 목록"

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MyAdapter()

        FirebaseDatabase.getInstance().getReference("/posts")
            .orderByChild("writeTime").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            if (previousChildName == null) {
                                posts.add(it)
                                recyclerView.adapter?.notifyItemInserted(posts.size-1)
                            } else {
                                val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                                posts.add(prevIndex+1, post)
                                recyclerView.adapter?.notifyItemInserted(prevIndex+1)
                            }
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                            posts[prevIndex+1] = post
                            recyclerView.adapter?.notifyItemChanged(prevIndex+1)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    snapshot?.let {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            val existIndex = posts.map { it.postId }.indexOf(it.postId)
                            posts.removeAt(existIndex)
                            recyclerView.adapter?.notifyItemRemoved(existIndex)
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            val existIndex = posts?.map { it.postId }.indexOf(it.postId)
                            posts.removeAt(existIndex)
                            recyclerView.adapter?.notifyItemRemoved(existIndex)
                            if (previousChildName == null) {
                                posts.add(it)
                                recyclerView.adapter?.notifyItemInserted(posts.size-1)
                            } else {
                                val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                                posts.add(prevIndex+1, post)
                                recyclerView.adapter?.notifyItemInserted(prevIndex+1)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    error?.toException()?.printStackTrace()
                }
            })
    }
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.bgImage
        val contentsText = itemView.contentsText
        val timeText = itemView.timeText
        val commentCountText = itemView.commentCountText
    }

    inner class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(
                LayoutInflater.from(this@MainActivity).inflate(R.layout.card_post, parent, false)
            )
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = posts[position]
            Picasso.get().load(Uri.parse(post.bgUri)).fit().centerCrop().into(holder.image)
            holder.contentsText.text = post.message
            holder.timeText.text = getDiffTimeText(post.writeTime as Long)
            holder.commentCountText.text = "0"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                intent.putExtra("postId", post.postId)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return posts.size
        }
    }

    private fun getDiffTimeText(targetTime: Long): String {
        val curDateTime = DateTime()
        val targetDateTime = DateTime().withMillis(targetTime)

        val diffDay = Days.daysBetween(curDateTime, targetDateTime).days
        val diffHours = Hours.hoursBetween(targetDateTime, curDateTime).hours
        val diffMinutes = Minutes.minutesBetween(targetDateTime, curDateTime).minutes
        if (diffDay == 0) {
            if (diffHours == 0 && diffMinutes == 0) {
                return "방금 전"
            }
            return if (diffHours > 0) {
                "${diffHours}시간 전"
            } else {
                "${diffMinutes}분 전"
            }
        } else {
            val format = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm")
            return format.format(Date(targetTime))
        }

    }
}