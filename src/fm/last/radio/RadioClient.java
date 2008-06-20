package fm.last.radio;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;

import org.kxmlrpc.XmlRpcClient;
import org.w3c.dom.Document;
import org.xml.sax.*;
	
import android.media.MediaPlayer;
import android.media.MediaPlayer.*;

import android.net.*;
import android.content.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.*;
import android.view.*;
import android.widget.*;

import fm.last.Application;
import fm.last.ImageLoader;
import fm.last.Log;
import fm.last.R;
import fm.last.TrackInfo;
import fm.last.R.id;
import fm.last.R.layout;
import fm.last.events.Event;


public class RadioClient extends Activity 
{
	private Radio m_radio = null;
	private Event m_event;
	private ImageLoader m_imageLoader;
	
	private enum Requests { Login } 

	/** Called when the activity is first created. */
	public void onCreate( Bundle icicle )
	{
		super.onCreate( icicle );
	 
		String user = Application.instance().userName();
		String pass = Application.instance().password();

		m_imageLoader = new ImageLoader(this);
		
		if( user.length() == 0 || pass.length() == 0 ) 
		{
			// show username / password activity
			startSubActivity( new Intent("ACCOUNTSETTINGS"), Requests.Login.ordinal() );
			return;
		}
		else	
			init();
	}
	
	protected void onActivityResult( int requestCode, int resultCode, String data, Bundle extras )
	{
		if( requestCode == Requests.Login.ordinal() )
			switch (resultCode)
			{
				case RESULT_OK:
					init();
				default:
					finish();
			}
	}
	
	RadioEventHandler m_radioEventHandler = new RadioEventHandler()
	{

		@Override
		public void onTrackEnded( TrackInfo track )
		{

		}

		@Override
		public void onTrackStarted( TrackInfo track )
		{
			setupUi( track );
		}
		
	};
	
	final private void init()
	{
		String user = Application.instance().userName();
		String pass = Application.instance().password();
		
		m_radio = new Radio( user, pass );	
		m_radio.addRadioHandler( m_radioEventHandler );

		setContentView( R.layout.radio_client );
		
		ViewInflate inflater = getViewInflate();
		View radioPartial = inflater.inflate( R.layout.event_radio_partial, null, null );
		LinearLayout radioLayout = (LinearLayout)findViewById( R.id.layout );
		radioPartial.setLayoutParams( new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, 
																	 LinearLayout.LayoutParams.WRAP_CONTENT) );
		radioLayout.addView( radioPartial, 0 );
		radioPartial.setVisibility( View.VISIBLE );
		radioPartial.setAnimation( AnimationUtils.loadAnimation( this, android.R.anim.slide_in_top ) );
		
		animate();
		
        ImageButton play = (ImageButton) findViewById( R.id.stop );
        play.setOnClickListener( new OnClickListener() 
        {
        	EditText edit = new EditText( RadioClient.this );
        	
            public void onClick( View v )
            {
            	edit.setLayoutParams( new LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT ) );
            	edit.setHint( "eg. Nirvana" );
            	edit.setSingleLine( true );
            	
                new AlertDialog.Builder( RadioClient.this )
                        .setTitle( "Similar Artist Radio" )
                        .setView( edit )
                        .setPositiveButton( "Tune-in", new DialogInterface.OnClickListener() 
                        {
                            public void onClick( DialogInterface dialog, int whichButton ) 
                            {
                                RadioClient.this.tuneInSimilarArtists( edit.getText().toString() );
                            }
                        })
                        .setNegativeButton( "Cancel", null )
                        .show();
            }
        });
        
        // check intent for event to begin playback
        ImageButton skip = (ImageButton) findViewById( R.id.skip );
        skip.setOnClickListener( new OnClickListener() 
        {
        	public void onClick( View v )
        	{
        		m_radio.skip();
        	}
        });
        
        Button info = (Button) findViewById( R.id.info );
        info.setOnClickListener( new OnClickListener()
        {
        	public void onClick( View v )
        	{
        		Intent i = new Intent( "MAP_ACTION" );
        		
        		Event e = RadioClient.this.m_event;
        		i.putExtra( "latitude", e.latitude() );
        		i.putExtra( "longitude", e.longitude() );
        		i.putExtra( "venue", e.venue() );
        		
        		startActivity( i );
        	}
        });
        
        readExtras();
	}
	
	private void readExtras()
	{
		final Bundle extras = getIntent().getExtras();
		if( extras.containsKey( "eventXml" ))
		{
			readEvent( extras.getString( "eventXml" ) );
		}
		else if( extras.containsKey( "tag" ) )
		{
			readTag( extras.getString( "tag" ) );
		}
	}
	
	private void readTag( String tag )
	{
		tuneInTag( tag );
	}
	
	private void readEvent( String eventXml )
	{
		m_event = Event.EventFromXmlString( eventXml );
		setupUi( m_event );
		tuneInSimilarArtists( m_event.headliner() );
	}

	final private void animate()
	{
        AnimationSet set = new AnimationSet( true );
		
        Animation animation = new AlphaAnimation( 0.0f, 1.0f );
        animation.setDuration( 1800 );
        set.addAnimation( animation );

        animation = new TranslateAnimation( Animation.RELATIVE_TO_SELF, 
        									-1.0f,
        									Animation.RELATIVE_TO_SELF, 
        									0.0f,
        									Animation.RELATIVE_TO_SELF, 
        									-1.0f,
        									Animation.RELATIVE_TO_SELF, 
        									0.0f );
        animation.setDuration( 500 );
        set.addAnimation( animation );

        LayoutAnimationController controller = new LayoutAnimationController( set, 0.5f );
        LinearLayout l = (LinearLayout) findViewById( R.id.layout );
        l.setLayoutAnimation( controller );
	}
	
	private void tuneInSimilarArtists( String artist )
	{
		Log.i( "Tuning-in..." );
		
		String stationName = m_radio.tuneToSimilarArtist( artist );
		
		TextView v = (TextView) findViewById( R.id.station_name );
		v.setText( stationName );
		
		m_radio.play();
	};

	private void tuneInTag( String tag )
	{
		Log.i( "Tuning-in..." );
		
		String stationName = m_radio.tuneToTag( tag );
		
		TextView v = (TextView) findViewById( R.id.station_name );
		v.setText( stationName );
		
		m_radio.play();
	};
	
	private void setupUi( Event e )
	{
		((TextView) findViewById( R.id.headliner )).setText( e.headliner() );
		((TextView) findViewById( R.id.venue )).setText( e.venue() );
	}
	
	private void setupUi( TrackInfo t )
	{
        TextView tv;
        tv = (TextView) findViewById( R.id.artist );
        tv.setText( t.artist() );
        tv = (TextView) findViewById( R.id.track_title );
        tv.setText( t.title() );
        
		ImageView v = (ImageView) findViewById( R.id.album_art );
		try {
			m_imageLoader.loadImage(v, albumArtUrl(t));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** kXMLRPC throws Exception from execute :( */
	private URL albumArtUrl( TrackInfo t ) throws Exception
	{
		XmlRpcClient client = new XmlRpcClient( "http://ws.audioscrobbler.com/1.0/rw/xmlrpc.php" );
	
		Vector<String> v = new Vector<String>( 4 );
		v.add( t.artist() );
		v.add( t.title() );
		v.add( t.album() );
		v.add( "en" );
		
		Map<String, String> m = (Map<String, String>) client.execute( "trackMetadata", v, this );
		return new URL( m.get( "albumCover" ) );
	}

	private OnBufferingUpdateListener onBufferUpdate = new OnBufferingUpdateListener()
	{
		public void onBufferingUpdate( MediaPlayer mp, int percent ) 
		{
			Log.i( "BufferUpdate: " + percent + "%" );
		}
	};

	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, "Account", new Runnable() {
			public void run() {
				startActivity( new Intent( "ACCOUNTSETTINGS" ) );
			}
		});

		menu.add(0, 1, "Events", new Runnable() {
			public void run() {
				startActivity( new Intent( "EVENTSVIEW" ) );
			}
		});
		
		return true;
	}
}
