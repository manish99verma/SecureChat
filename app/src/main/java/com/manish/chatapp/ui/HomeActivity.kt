package com.manish.chatapp.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.manish.chatapp.R
import com.manish.chatapp.data.model.User
import com.manish.chatapp.databinding.ActivityHomeBinding
import com.manish.chatapp.ui.adapter.UsersAdapter
import com.manish.chatapp.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: UsersAdapter
    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setUpRecyclerView()

        binding.menuIcon.setOnClickListener {
            showMenu(it, R.menu.home_menu)
        }
    }

    private fun setUpRecyclerView() {
        adapter = UsersAdapter()
        binding.rvHome.layoutManager = LinearLayoutManager(this)
        binding.rvHome.adapter = adapter
        binding.rvHome.setHasFixedSize(true)

        viewModel.getUsers().observe(this) {
            if (it.error != null) {
                Toast.makeText(this@HomeActivity, it.error.message, Toast.LENGTH_SHORT).show()
            } else {
                Log.d("TAGY", "setUpRecyclerView: ${it.data}")
                it.data?.let { it1 ->
                    adapter.setList(it1)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        adapter.setOnClickListener { otherUser ->
            val chatIntent = Intent(this@HomeActivity, ChatActivity::class.java)

            // Put user
            val currUser: User? = viewModel.getCurrDatabaseUser()
            if (currUser == null) {
                Log.d("TAGY", "setUpRecyclerView: Null Curr User!")
                return@setOnClickListener
            }

            chatIntent.putExtra("curr_user", currUser)
            chatIntent.putExtra("other_user", otherUser)

            startActivity(chatIntent)
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(this, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            if (menuItem.itemId == R.id.log_out) {
                viewModel.logOut()

                //Restart Activity
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)

                    val pm: PackageManager = getPackageManager()
                    val intent = pm.getLaunchIntentForPackage(getPackageName())
                    val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
                    startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                }
            }
            return@setOnMenuItemClickListener true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }
}