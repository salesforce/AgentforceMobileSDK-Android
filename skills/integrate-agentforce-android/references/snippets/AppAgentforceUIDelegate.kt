// Default AgentforceUIDelegate. Hook your analytics or message-rewriting
// logic into the methods below; remove what you don't need.

package {{PACKAGE}}.agentforce

import android.util.Log
import com.salesforce.android.agentforcesdkimpl.AgentConversation
import com.salesforce.android.agentforcesdkimpl.AgentforceUIDelegate
import com.salesforce.android.agentforceservice.AgentforceUtterance

class AppAgentforceUIDelegate : AgentforceUIDelegate {

    override suspend fun modifyUtteranceBeforeSending(
        agentforceUtterance: AgentforceUtterance
    ): AgentforceUtterance {
        // TODO: rewrite or annotate the utterance before it leaves the device.
        return agentforceUtterance
    }

    override fun didSendUtterance(agentforceUtterance: AgentforceUtterance) {
        // TODO: track in analytics.
        Log.d("AgentforceUIDelegate", "didSendUtterance: ${agentforceUtterance.utterance}")
    }

    override fun userDidSwitchAgents(newConversation: AgentConversation) {
        // TODO: react to multi-agent handoff (e.g. update header, log event).
    }
}
