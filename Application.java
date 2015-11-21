package org.red5.demos.oflaDemo;

import java.io.File;
import java.io.PrintStream;
import java.sql.*;
import java.util.*;
import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IServerStream;
import org.slf4j.Logger;

public class Application extends ApplicationAdapter
{

    public Application()
    {
    }

    public boolean appStart(IScope app)
    {
        super.appStart(app);
        log.info("oflaDemo appStart");
        System.out.println("oflaDemo appStart");
        appScope = app;
        return true;
    }

    public boolean appConnect(IConnection conn, Object params[])
    {
        log.info("oflaDemo appConnect");
        IScope appScope = conn.getScope();
        log.debug("App connect called for scope: {}", appScope.getName());
        Map properties = conn.getConnectParams();
        if(log.isDebugEnabled())
        {
            java.util.Map.Entry e;
            for(Iterator iterator = properties.entrySet().iterator(); iterator.hasNext(); log.debug("Connection property: {} = {}", e.getKey(), e.getValue()))
                e = (java.util.Map.Entry)iterator.next();

        }
        return super.appConnect(conn, params);
    }

    public void appDisconnect(IConnection conn)
    {
        log.info("oflaDemo appDisconnect");
        if(appScope == conn.getScope() && serverStream != null)
            serverStream.close();
        super.appDisconnect(conn);
    }

    public void streamBroadcastStart(IBroadcastStream stream)
    {
        log.debug("Start Hello", "OMAR SAYED");
    }

    public void streamPublishStart(IBroadcastStream stream)
    {
        IConnection connection = Red5.getConnectionLocal();
        log.info("W3C x-category:stream x-event:publish c-ip:{} x-sname:{} x-name:{}", new Object[] {
            connection == null ? "0.0.0.0" : connection.getRemoteAddress(), stream.getName(), stream.getPublishedName()
        });
    }

    public void streamRecordStart(IBroadcastStream stream)
    {
        IConnection connection = Red5.getConnectionLocal();
        log.info("W3C x-category:stream x-event:record-start c-ip:{} x-sname:{} x-file-name:{}", new Object[] {
            connection == null ? "0.0.0.0" : connection.getRemoteAddress(), stream.getName(), stream.getSaveFilename()
        });
    }

    public void streamRecordStop(IBroadcastStream stream)
    {
        IConnection connection = Red5.getConnectionLocal();
        log.info("W3C x-category:stream x-event:record-stop c-ip:{} x-sname:{} x-file-name:{}", new Object[] {
            connection == null ? "0.0.0.0" : connection.getRemoteAddress(), stream.getName(), stream.getSaveFilename()
        });
    }

    public void streamBroadcastClose(IBroadcastStream stream)
    {
        IConnection conn = Red5.getConnectionLocal();
        long publishDuration = (System.currentTimeMillis() - stream.getCreationTime()) / 1000L;
        if(conn != null)
            log.info("W3C x-category:stream x-event:unpublish c-ip:{} cs-bytes:{} sc-bytes:{} x-sname:{} x-file-length:{} x-name:{}", new Object[] {
                conn.getRemoteAddress(), Long.valueOf(conn.getReadBytes()), Long.valueOf(conn.getWrittenBytes()), stream.getName(), Long.valueOf(publishDuration), stream.getPublishedName()
            });
        else
            log.info("W3C x-category:stream x-event:unpublish x-sname:{} x-file-length:{} x-name:{}", new Object[] {
                stream.getName(), Long.valueOf(publishDuration), stream.getPublishedName()
            });
        String filename = stream.getPublishedName();
        String parts[] = filename.split("_");
        String id = parts[2];
        String userid = parts[3];
        String recordingName = stream.getSaveFilename();
        if(recordingName != null)
        {
            if(conn != null)
            {
                try
                {
                    Connection mySQLConn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306", "osm", "Admin2014");
                    Statement sttmnt = mySQLConn.createStatement();
                    String insert = (new StringBuilder()).append("INSERT INTO `admin_medical`.`wp_js_ticket_replies` (`uid`,`ticketid`)VALUES(").append(userid).append(", ").append(id).append(")").toString();
                    sttmnt.executeUpdate(insert, 1);
                    ResultSet genKeys = sttmnt.getGeneratedKeys();
                    if(genKeys.next())
                    {
                        int lastid = genKeys.getInt(1);
                        Statement sttmnt2 = mySQLConn.createStatement();
                        String insert2 = (new StringBuilder()).append("INSERT INTO `admin_medical`.`wp_js_ticket_attachments` (`ticketid`,`replyattachmentid`,`filename`)VALUES(").append(id).append(", ").append(lastid).append(", '").append(recordingName).append("')").toString();
                        sttmnt2.executeUpdate(insert2);
                    }
                }
                catch(SQLException exception)
                {
                    log.error((new StringBuilder()).append("SQLException at appConnect: ").append(exception.getMessage()).toString());
                }
                log.info("W3C x-category:stream x-event:recordstop c-ip:{} cs-bytes:{} sc-bytes:{} x-sname:{} x-file-name:{} x-file-length:{} x-file-size:{}", new Object[] {
                    conn.getRemoteAddress(), Long.valueOf(conn.getReadBytes()), Long.valueOf(conn.getWrittenBytes()), stream.getName(), recordingName, Long.valueOf(publishDuration), Long.valueOf(conn.getReadBytes())
                });
            } else
            {
                log.info(" W3C x-category:stream x-event:recordstop  x-sname:{} x-file-name:{} x-file-length:{}", new Object[] {
                    stream.getName(), recordingName, Long.valueOf(publishDuration)
                });
            }
            String webappsPath = System.getProperty("red5.webapp.root");
            File file = new File(webappsPath, (new StringBuilder()).append(getName()).append('/').append(recordingName).toString());
            if(file != null)
            {
                log.debug("File path: {}", file.getAbsolutePath());
                if(file.exists() && (publishDuration == 0L || file.length() == 0L))
                    if(file.delete())
                    {
                        log.info("File {} was deleted", file.getName());
                    } else
                    {
                        log.info("File {} was not deleted, it will be deleted on exit", file.getName());
                        file.deleteOnExit();
                    }
                file = null;
            }
        }
    }

    private IScope appScope;
    private IServerStream serverStream;
    private static final String url = "jdbc:mysql://127.0.0.1:3306";
    private static final String user = "osm";
    private static final String pass = "Admin2014";

    static 
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch(ClassNotFoundException exception)
        {
            System.out.println((new StringBuilder()).append("ClassNotFoundException ").append(exception.getMessage()).toString());
        }
    }
}
