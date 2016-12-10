/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package audio.broadcast.icecast;

import alias.AliasModel;
import audio.broadcast.BroadcastConfiguration;
import audio.broadcast.BroadcastConfigurationEditor;
import audio.broadcast.BroadcastEvent;
import audio.broadcast.BroadcastModel;
import audio.broadcast.BroadcastServerType;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class IcecastHTTPConfigurationEditor extends BroadcastConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger( IcecastHTTPConfigurationEditor.class );

    private JTextField mName;
    private JTextField mServer;
    private JTextField mPort;
    private JTextField mMountPoint;
    private JTextField mUsername;
    private JTextField mPassword;
    private JButton mSaveButton;
    private JButton mResetButton;
    private JCheckBox mEnabled;

    public IcecastHTTPConfigurationEditor(BroadcastModel broadcastModel, AliasModel aliasModel,
                                          SettingsManager settingsManager)
    {
        super(broadcastModel, aliasModel, settingsManager);
        init();
    }

    private void init()
    {
        setLayout( new MigLayout( "fill,wrap 2", "[align right][grow,fill]", "[][][][][][][][][][grow,fill]" ) );
        setPreferredSize(new Dimension(150,400));

        JLabel channelLabel = new JLabel("Icecast 2 (v2.4+) Stream");

        ImageIcon icon = mSettingsManager.getImageIcon(BroadcastServerType.ICECAST_HTTP.getIconName(), 25);
        channelLabel.setIcon(icon);

        add(channelLabel, "span, align center");

        add(new JLabel("Name:"));
        mName = new JTextField();
        mName.getDocument().addDocumentListener(this);
        mName.setToolTipText("Unique name for this stream");
        add(mName);

        add(new JLabel("Server:"));
        mServer = new JTextField();
        mServer.getDocument().addDocumentListener(this);
        mServer.setToolTipText("Server address (e.g. audio1.icecast.com or localhost)");
        add(mServer);

        add(new JLabel("Port:"));
        mPort = new JTextField();
        mPort.getDocument().addDocumentListener(this);
        mPort.setToolTipText("Server port number (e.g. 80)");
        add(mPort);

        add(new JLabel("Mount:"));
        mMountPoint = new JTextField();
        mMountPoint.getDocument().addDocumentListener(this);
        mMountPoint.setToolTipText("Mount point (e.g. \\12345)");
        add(mMountPoint);

        add(new JLabel("User Name:"));
        mUsername = new JTextField();
        mUsername.getDocument().addDocumentListener(this);
        mUsername.setToolTipText("User name for the stream. Default: source");
        add(mUsername);

        add(new JLabel("Password:"));
        mPassword = new JTextField();
        mPassword.getDocument().addDocumentListener(this);
        mPassword.setToolTipText("Password for the stream");
        add(mPassword);

        mEnabled = new JCheckBox("Enabled");
        mEnabled.setToolTipText("Enable (checked) or disable this stream");
        mEnabled.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mEnabled, "span");

        mSaveButton = new JButton("Save");
        mSaveButton.setEnabled(false);
        mSaveButton.setToolTipText("Click to save configuration information");
        mSaveButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                save();
            }
        });
        add(mSaveButton);

        mResetButton = new JButton("Reset");
        mResetButton.setEnabled(false);
        mResetButton.setToolTipText("Click to reset any changes you've made since the last save");
        mResetButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(false);
                setItem(getItem());
            }
        });
        add(mResetButton);
    }

    @Override
    public void setModified(boolean modified)
    {
        super.setModified(modified);

        mSaveButton.setEnabled(modified);
        mResetButton.setEnabled(modified);
    }

    @Override
    public void setItem(BroadcastConfiguration item)
    {
        super.setItem(item);

        if(hasItem())
        {
            IcecastHTTPConfiguration config = (IcecastHTTPConfiguration)getItem();

            mName.setText(config.getName());
            mServer.setText(config.getHost());
            mPort.setText(config.getPort() != 0 ? String.valueOf(config.getPort()) : null);
            mMountPoint.setText(config.getMountPoint());
            mUsername.setText(config.getUserName());
            mPassword.setText(config.getPassword());
            mEnabled.setSelected(config.isEnabled());
        }
        else
        {
            mName.setText(null);
            mServer.setText(null);
            mPort.setText(null);
            mMountPoint.setText(null);
            mUsername.setText(null);
            mPassword.setText(null);
            mEnabled.setSelected(false);
        }

        setModified(false);
    }

    @Override
    public void save()
    {
        IcecastHTTPConfiguration config = (IcecastHTTPConfiguration)getItem();

        if(validateConfiguration())
        {
            updateConfigurationName(config, mName.getText());
            config.setHost(mServer.getText());
            config.setPort(getPort());
            config.setMountPoint(mMountPoint.getText());
            config.setUserName(mUsername.getText());
            config.setPassword(mPassword.getText());
            config.setEnabled(mEnabled.isSelected());

            setModified(false);

            mBroadcastModel.process(new BroadcastEvent(getItem(), BroadcastEvent.Event.CONFIGURATION_CHANGE));
        }
    }

    /**
     * Validates the user specified values in the controls prior to saving them to the configuration
     */
    private boolean validateConfiguration()
    {
        String name = mName.getText();

        //A unique stream name is required
        if(!mBroadcastModel.isUniqueName(name, getItem()))
        {
            String message = (name == null || name.isEmpty()) ?
                    "Please specify a stream name." : "Stream name " + name +
                    "is already in use.\nPlease choose another name.";

            JOptionPane.showMessageDialog(IcecastHTTPConfigurationEditor.this,
                    message, "Invalid Stream Name", JOptionPane.ERROR_MESSAGE);

            mName.requestFocus();

            return false;
        }

        //Server address is optional, but required
        if(!validateTextField(mServer, "Invalid Server Address", "Please specify a server address."))
        {
            return true;
        }

        //Port is optional, but required
        if(!validateIntegerTextField(mPort, "Invalid Port Number", "Please specify a port number (1 <> 65535)", 1, 65535))
        {
            return true;
        }

        //Mount Point is required
        if(!validateTextField(mMountPoint, "Invalid Mount Point", "Please specify a mount point."))
        {
            return false;
        }

        //User name is required
        if(!validateTextField(mUsername, "Invalid User Name", "Please specify a user name."))
        {
            return false;
        }

        //Password is required
        if(!validateTextField(mPassword, "Invalid Password", "Please specify a password."))
        {
            return false;
        }

        return true;
    }

    /**
     * Parses an Integer from the port text control or returns 0 if the control doesn't contain an integer value.
     */
    private int getPort()
    {
        String port = mPort.getText();

        if(port != null && !port.isEmpty())
        {
            try
            {
                return Integer.parseInt(port);
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the number value
            }
        }

        return 0;
    }
}
