package com.example.todo.intro.intro.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.todo.databinding.FragmentIndexBinding
import com.example.todo.intro.intro.EditTask.TaskScreenActivity
import com.example.todo.intro.intro.adapters.tasksAdapter
import com.example.todo.intro.intro.database.getTasksFromFireStore
import com.example.todo.intro.intro.database.getUserFromFirestore
import com.example.todo.intro.intro.database.taskData
import com.example.todo.intro.intro.database.userData
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import java.util.Date


class indexFragment : Fragment() {
    lateinit var adapter: tasksAdapter
    lateinit var dataBinding: FragmentIndexBinding
    lateinit var auth: FirebaseAuth
    lateinit var updatedTaskData: taskData
    private val EDIT_TASK_REQUEST_CODE = 1 // You can use any unique request code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = FragmentIndexBinding.inflate(inflater,container,false)
        return dataBinding.root



    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }
    private fun initView() {
        auth = Firebase.auth
        adapter = tasksAdapter(taskData.tasksList)
        dataBinding.rvTasks.adapter = adapter
        getTasksFromFireStoreDB(auth.currentUser?.uid!!)
        // Inside your click listener or wherever you start TaskScreenActivity
        adapter.onTaskItemClickListener = object : tasksAdapter.OnTaskItemClickListener {
            override fun onItemClick(list: List<taskData>, position: Int) {
                val edit_task = list[position]
                val edit_task_date = list[position].time
                val edit_task_id = list[position].id
                val intent = Intent(activity, TaskScreenActivity::class.java)
                intent.putExtra("position", position)
                intent.putExtra("edit_task", edit_task)
                intent.putExtra("edit_task_date", edit_task_date)
                intent.putExtra("edit_task_id", edit_task_id)
                startActivityForResult(intent, EDIT_TASK_REQUEST_CODE)
            }
        }
        adapter.onImageClickListener = object : tasksAdapter.OnImageClickListener{
            override fun onImageClick(list: List<taskData>, position: Int) {
                var item = list[position]
                item.isDone = true
                adapter.notifyItemChanged(position)
//                updateTaskInFirestore(item)
            }

        }
    }

    private fun getTasksFromFireStoreDB(userID: String) {
        getTasksFromFireStore(
            userId = userID,
            listener = { snapshots, e ->
                if (e != null) {
                    Log.w("TAG", "listen:error", e)
                    return@getTasksFromFireStore
                }

                val modifiedTasks = mutableListOf<taskData>()

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            taskData.tasksList.add(dc.document.toObject(taskData::class.java))
                            modifiedTasks.add(dc.document.toObject(taskData::class.java))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // Extract the modified task data
                            val modifiedTask = dc.document.toObject(taskData::class.java)

                            // Update the corresponding task in the list
                            val position = taskData.tasksList.indexOfFirst { it.id == modifiedTask.id }
                            if (position != -1) {
                                taskData.tasksList[position] = modifiedTask
                            }

                            modifiedTasks.add(modifiedTask)
                        }
                        else -> {}
                    }
                }

                // Update the adapter with only the modified tasks
                adapter.updateAdapter(modifiedTasks)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        getTasksFromFireStoreDB(auth.currentUser?.uid!!)
    }

    override fun onStart() {
        super.onStart()
        getTasksFromFireStoreDB(auth.currentUser?.uid!!)

    }
}
