package info.nightscout.androidaps.plugins.general.wear.wearintegration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import info.nightscout.androidaps.interfaces.NotificationHolder
import javax.inject.Inject
import javax.inject.Singleton

/*
    This code replaces  following
    val alarm = Intent(context, DataLayerListenerServiceMobile::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(alarm) else context.startService(alarm)

    it fails randomly with error
    Context.startForegroundService() did not then call Service.startForeground(): ServiceRecord{e317f7e u0 info.nightscout.nsclient/info.nightscout.androidaps.services.DataLayerListenerServiceMobile}

 */
@Singleton
class DataLayerListenerServiceMobileHelper @Inject constructor(
    private val notificationHolder: NotificationHolder
) {

    fun startService(context: Context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                // The binder of the service that returns the instance that is created.
                val binder: DataLayerListenerServiceMobile.LocalBinder = service as DataLayerListenerServiceMobile.LocalBinder

                val dataLayerListenerServiceMobile: DataLayerListenerServiceMobile = binder.getService()

                context.startForegroundService(Intent(context, DataLayerListenerServiceMobile::class.java))

                // This is the key: Without waiting Android Framework to call this method
                // inside Service.onCreate(), immediately call here to post the notification.
                dataLayerListenerServiceMobile.startForeground(notificationHolder.notificationID, notificationHolder.notification)

                // Release the connection to prevent leaks.
                context.unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
            }
        }

        try {
            context.bindService(Intent(context, DataLayerListenerServiceMobile::class.java), connection, Context.BIND_AUTO_CREATE)
        } catch (ignored: RuntimeException) {
            // This is probably a broadcast receiver context even though we are calling getApplicationContext().
            // Just call startForegroundService instead since we cannot bind a service to a
            // broadcast receiver context. The service also have to call startForeground in
            // this case.
            context.startForegroundService(Intent(context, DataLayerListenerServiceMobile::class.java))
        }
    }

    fun stopService(context: Context) {
        context.stopService(Intent(context, DataLayerListenerServiceMobile::class.java))
    }
}