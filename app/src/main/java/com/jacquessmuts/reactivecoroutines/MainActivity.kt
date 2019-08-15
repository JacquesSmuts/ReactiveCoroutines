package com.jacquessmuts.reactivecoroutines

import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.view.View

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private var fabChannel = Channel<Unit>(1)

    private var buttonChannel = Channel<Unit>(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

//        launch {
//            while (true){
//                fabChannel.receive()
//
//                Snackbar.make(fab, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//            }
//        }

        fab.bindClick(fabChannel)
//        button.bindClick(buttonChannel)

        fabChannel.subscribeThrottled(this) {
            Snackbar.make(fab, "1 sec Delay", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }


        button.clicks().subscribe(this, Dispatchers.Default) {
            Snackbar.make(fab, "Normal subscribe", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

    }

    override fun onDestroy() {
        Log.d("MainActivity", "cancelling children")
        coroutineContext.cancelChildren()
        super.onDestroy()
    }
}

fun View.clicks(): Channel<Unit> {
    return ViewClicksChannel(this).channel
}

@ExperimentalCoroutinesApi
private class ViewClicksChannel(private val view: View) {

    val channel = Channel<Unit>()

    init {
        view.setOnClickListener {
            if (!channel.isClosedForSend)
                channel.offer(Unit)
        }

        channel.invokeOnClose {
            view.setOnClickListener(null)
        }
    }



}

fun View.bindClick(channel: Channel<Unit>) {
    this.setOnClickListener {
        if (!channel.isClosedForSend)
            channel.offer(Unit)
    }
}

fun<T> Channel<T>.subscribe (coroutineScope: CoroutineScope, dispatcher: CoroutineContext = EmptyCoroutineContext, action: () -> Unit) {

    coroutineScope.launch(dispatcher) {
        try {
            while (true) {
                receive()
                action()
            }
        } finally {
            Log.e("ChannelSubscribe", "Cancellation received")
            cancel()
        }
    }

}

fun<T> Channel<T>.subscribeThrottled (coroutineScope: CoroutineScope, throttleTime: Long = 1000, action: () -> Unit) {

    require(throttleTime >= 0)

    coroutineScope.launch {
        while (true) {
            receive()
            action()
            delay(throttleTime)
        }
    }
}
