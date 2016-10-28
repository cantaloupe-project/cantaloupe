/**************************************************************************
/* This interface defines methods for a ProcessEventListener.
/*
/* Copyright (c) 2010 by Bernhard Bablok (mail@bablokb.de)
/*
/* This program is free software; you can redistribute it and/or modify
/* it under the terms of the GNU Library General Public License as published
/* by  the Free Software Foundation; either version 2 of the License or
/* (at your option) any later version.
/*
/* This program is distributed in the hope that it will be useful, but
/* WITHOUT ANY WARRANTY; without even the implied warranty of
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/* GNU Library General Public License for more details.
/*
/* You should have received a copy of the GNU Library General Public License
/* along with this program; see the file COPYING.LIB.  If not, write to
/* the Free Software Foundation Inc., 59 Temple Place - Suite 330,
/* Boston, MA  02111-1307 USA
/**************************************************************************/

package org.im4java.process;

/**
   This interface defines methods for a ProcessEventListener. This interface
   replaces the deprecated interface ProcessListener.

   @version $Revision: 1.2 $
   @author  $Author: bablokb $
 
   @since 1.1.0
 */

public interface ProcessEventListener {

  //////////////////////////////////////////////////////////////////////////////

  /**
     This method is called at process initiation.
  */

  public void processInitiated(ProcessEvent pEvent);

  //////////////////////////////////////////////////////////////////////////////

  /**
     This method is called at process startup.
  */

  public void processStarted(ProcessEvent pEvent);

  //////////////////////////////////////////////////////////////////////////////

  /**
     This method is called at normal or abnormal process termination.
  */

  public void processTerminated(ProcessEvent pEvent);
}
