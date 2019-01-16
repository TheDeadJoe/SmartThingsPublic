/**
 *	Author: Robert Tokarski (TheDeadJoe)
 *	email: the.dead.joe@gmail.com
 *	Date: 2018-12-20
 */
 
metadata {
    definition(name: "Shelly Relay", namespace: "TheDeadJoe", author: "Robert Tokarski") {
		capability "Switch"
        capability "Refresh"
    }

	tiles {  
    	standardTile("switch", "device.switch", canChangeIcon: true) {
        	state "off", label: "off", action: "on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        	state "on", label: "on", action: "off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
    	}
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label: "", action: "refresh", icon:"st.secondary.refresh"
        }  
        
        main(["switch"])
        
        details(["switch", "refresh"])  
    }
}

def on() {
	parent.childOn(device.deviceNetworkId)
}

def off() {
	parent.childOff(device.deviceNetworkId)
}

def refresh() {
	parent.refresh()
}