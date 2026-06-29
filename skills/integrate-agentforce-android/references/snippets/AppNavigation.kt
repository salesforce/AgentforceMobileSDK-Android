// No-op Navigation implementation. Replace with real navigation logic
// when you want the agent to open records or app screens — e.g. parse
// the destination URL and dispatch to your Navigation Compose graph.

package {{PACKAGE}}.agentforce

import com.salesforce.android.mobile.interfaces.navigation.Navigation
import com.salesforce.android.mobile.interfaces.navigation.destination.App
import com.salesforce.android.mobile.interfaces.navigation.destination.Destination

class AppNavigation : Navigation {

    override fun goto(destination: Destination) {
        // TODO: handle navigation requests from the agent.
    }

    override fun goto(destination: Destination, replace: Boolean) {
        // TODO: handle navigation requests from the agent (with replace semantics).
    }

    override fun openApp(app: App): App.OpenResult = App.OpenResult.NOTOPEN
}
