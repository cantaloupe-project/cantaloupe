/**************************************************************************
/* This class is a container for objects logically wrapping stdin, stdout
/* and stderr.
/*
/* Copyright (c) 2009 by Bernhard Bablok (mail@bablokb.de)
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
   This class is a container for objects logically wrapping stdin, stdout
   and stderr.

   @version $Revision: 1.2 $
   @author  $Author: bablokb $
 
   @since 0.95
*/

public class StandardStream {

  //////////////////////////////////////////////////////////////////////////////

  /**
     InputProvider wrapping System.in.
  */

  public static final InputProvider STDIN = new Pipe(System.in,null);

  //////////////////////////////////////////////////////////////////////////////

  /**
     OutputConsumer wrapping System.out.
  */

  public static final OutputConsumer STDOUT = new Pipe(null,System.out);

  //////////////////////////////////////////////////////////////////////////////

  /**
     ErrorConsumer wrapping System.err.
  */

  public static final ErrorConsumer STDERR = new Pipe(null,System.err);

 //////////////////////////////////////////////////////////////////////////////

  /**
     Private Constructor. Since this is just a container for predefined
     objects, there is no need to instantiate this class.
  */

  private StandardStream() {
  }
}
