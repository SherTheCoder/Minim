package ddf.minim.ugens;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ddf.minim.Minim;

/**
 * A <code>Frequency</code> is used to contain a frequency.
 * This is generally used by an <code>Oscil</code> UGen, but
 * can also be used to convert different notations of frequencies
 * such as Hz, MIDI note number, and a pitch name (English or solfege).
 *  
 * @author nodog
 *
 */
public class Frequency
{
	static float HZA4=440.0f;
	static float MIDIA4=69.0f;
	static float MIDIOCTAVE=12.0f;
	
	// A TreeMap is used to force order so that later when creating the regex for
	// the note names, an ordered list can be used.
	private static TreeMap< String, Integer > noteNameOffsets = initializeNoteNameOffsets();
	private static TreeMap< String, Integer > initializeNoteNameOffsets()
	{
		TreeMap< String, Integer > initNNO = new TreeMap< String, Integer >();
		initNNO.put( "A", new Integer( 9 ) );
		initNNO.put( "B", new Integer( 11 ) );
		initNNO.put( "C", new Integer( 0 ) );
		initNNO.put( "D", new Integer( 2 ) );
		initNNO.put( "E", new Integer( 4 ) );
		initNNO.put( "F", new Integer( 5 ) );
		initNNO.put( "G", new Integer( 7 ) );
		initNNO.put( "La", new Integer( 9 ) );
		initNNO.put( "Si", new Integer( 11 ) );
		//initNNO.put( "Ti", new Integer( 11 ) );
		initNNO.put( "Do", new Integer( 0 ) );
		//initNNO.put( "Ut", new Integer( 0 ) );
		initNNO.put( "Re", new Integer( 2 ) );
		initNNO.put( "Mi", new Integer( 4 ) );
		initNNO.put( "Fa", new Integer( 5 ) );
		initNNO.put( "Sol", new Integer( 7 ) );
		return initNNO;
	}

	// several regex expression are used in determining the Frequency of musical pitches
	// want to build up the regex from components of noteName, noteNaturalness, and noteOctave
	private static String noteNameRegex = initializeNoteNameRegex();
	private static String initializeNoteNameRegex()
	{
		// noteName is built using the keys from the noteNameOffsets hashmap
		// The reverserList is a bit ridiculous, but necessary to reverse the 
		// order of the the keys so that Do and Fa come before D and F.
		// (There is no .previous() method for a regular Iterator.)
		ArrayList< String > reverserList = new ArrayList< String >();
		Iterator< String > iterator = noteNameOffsets.keySet().iterator();
		while( iterator.hasNext() )
		{
			reverserList.add( iterator.next() );	
		}
		// so that Do comes before D and is found first.
		String nNR = "(";
		ListIterator< String > listIterator = reverserList.listIterator( reverserList.size() );
		while( listIterator.hasPrevious() )
		{
			nNR += listIterator.previous() + "|";
		}
		// remove last | or empty string is included
		nNR = nNR.substring( 0, nNR.length() - 1 );
		nNR += ")";
		return nNR;
	}
	
	private static String noteNaturalnessRegex = "[#b]";
	private static String noteOctaveRegex = "(-1|10|[0-9])";
	private static String pitchRegex = "^" + noteNameRegex 
			+ "?[ ]*" + noteNaturalnessRegex + "*[ ]*" + noteOctaveRegex +"?$";

	private float freq;
	
	// The constructors are way down here.
	Frequency( float hz )
	{
		freq = hz;
	}

	Frequency( String pitchName )
	{
		freq = Frequency.ofPitch( pitchName ).asHz();
	}
	
	public float asHz()
	{
		return freq;
	}
	
	public float asMidiNote()
	{
		float midiNote = MIDIA4 + MIDIOCTAVE*(float)Math.log( freq/HZA4 )/(float)Math.log( 2.0 );
		return midiNote;
	}
	
	public static Frequency ofHertz(float hz)
	{
		return new Frequency(hz);
	}
	
	public static Frequency ofMidiNote( float midiNote )
	{
		float hz = HZA4*(float)Math.pow( 2.0, ( midiNote - MIDIA4 )/MIDIOCTAVE );
		return new Frequency(hz);
	}
	
	public static Frequency ofPitch(String pitchName)
	{
		// builds up the value of a midiNote used to create the returned Frequency
		float midiNote;
		
		// trim off any white space before or after
		pitchName = pitchName.trim();
	
		// check to see if this is a note		
		if ( pitchName.matches( pitchRegex ) )
		{
			Minim.debug(pitchName + " matches the pitchRegex.");
			float noteOctave;
			
			// get octave
			Pattern pattern = Pattern.compile( noteOctaveRegex );
			Matcher matcher = pattern.matcher( pitchName );
			
			if ( matcher.find() )
			{
				String octaveString = pitchName.substring( matcher.start(), matcher.end() );
				noteOctave = Float.valueOf( octaveString.trim() ).floatValue();
			} else  // default octave of 4
			{
				noteOctave = 4.0f;
			}
			midiNote = noteOctave*12.0f + 12.0f;
			Minim.debug("midiNote based on octave = " + midiNote );

			// get naturalness			
			pattern = Pattern.compile( noteNaturalnessRegex );
			matcher = pattern.matcher( pitchName );
			
			while( matcher.find() )
			{
				String naturalnessString = pitchName.substring(matcher.start(), matcher.end() );
				if ( naturalnessString.equals("#") )
				{
					midiNote += 1.0f;
				} else  // must be a "b"
				{
					midiNote -= 1.0f;
				}
			}
			Minim.debug("midiNote based on naturalness = " + midiNote );
	
			// get note
			pattern = Pattern.compile( noteNameRegex );
			matcher = pattern.matcher( pitchName );
			
			if ( matcher.find() )
			{	
				String noteNameString = pitchName.substring(matcher.start(), matcher.end() );
				float noteOffset = (float) noteNameOffsets.get( noteNameString );
				midiNote += noteOffset;
			}
			Minim.debug("midiNote based on noteName = " + midiNote );

			// return a Frequency object with this midiNote
			return new Frequency( ofMidiNote( midiNote ).asHz() );
					
		} else  // string does not conform to note name syntax
		{
			Minim.debug(pitchName + " DOES NOT MATCH.");			
			// return a Frequency object of 0.0 Hz.
			return new Frequency( 0.0f );
		}
	}
}