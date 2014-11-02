package grails.plugin.wschat


import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import javax.websocket.EndpointConfig
import javax.websocket.OnClose
import javax.websocket.OnError
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.server.PathParam
import javax.websocket.server.ServerContainer
import javax.websocket.server.ServerEndpoint

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@WebListener
@ServerEndpoint("/WsChatEndpoint/{room}")
class WsChatEndpoint extends ChatUtils implements ServletContextListener {
	private final Logger log = LoggerFactory.getLogger(getClass().name)
	
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext servletContext = event.servletContext
		final ServerContainer serverContainer = servletContext.getAttribute("javax.websocket.server.ServerContainer")
		try {
			serverContainer.addEndpoint(WsChatEndpoint)

			def ctx = servletContext.getAttribute(GA.APPLICATION_CONTEXT)
			
			grailsApplication = ctx.grailsApplication

			def config = grailsApplication.config
			int defaultMaxSessionIdleTimeout = config.wschat.timeout ?: 0
			serverContainer.defaultMaxSessionIdleTimeout = defaultMaxSessionIdleTimeout
		}
		catch (IOException e) {
			log.error e.message, e
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}

	@OnOpen
	public void handleOpen(Session userSession,EndpointConfig c,@PathParam("room") String room) {
		chatroomUsers.add(userSession)
		userSession.userProperties.put("room", room)
		
		def ctx= SCH.servletContext.getAttribute(GA.APPLICATION_CONTEXT)
		grailsApplication= ctx.grailsApplication
		
	}

	@OnMessage
	public String handleMessage(String message,Session userSession) throws IOException {
		try {
			verifyAction(userSession,message)
		} catch(IOException e) {
			log.debug "Error $e"
		}
	}

	@OnClose
	public void handeClose(Session userSession) throws SocketException {
		try {
			String username=userSession?.userProperties?.get("username")
			if (dbSupport()&&username) {
				validateLogOut(username as String)
			}
		} catch(SocketException e) {
			log.debug "Error $e"
		}
	}

	@OnError
	public void handleError(Throwable t) {
		t.printStackTrace()
	}






}
