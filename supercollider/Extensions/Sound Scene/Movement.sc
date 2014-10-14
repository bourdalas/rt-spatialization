////////////////////////////////////////////////////////////////////////////
//
// Copyright ANDRÉS PÉREZ LÓPEZ, April 2014 [contact@andresperezlopez.com]
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; withot even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>
//
////////////////////////////////////////////////////////////////////////////
//
// Movement.sc
//
// This class implements some discrete dynamic behaviors for the Sound Scene simulation
//
// Each instance is associated with the SSObject instance that has this concrete behavior
//
// TODO:
// -> support several dynamic behaviors (http://redmine.spatdif.org/projects/spatdif/wiki/Trajectory_Extension)
// -> implement as Patterns (what to do with input forces??)
// -> implement Orbit, Brownian compatibility with addForce
//
////////////////////////////////////////////////////////////////////////////

Movement {

	var <>object; // a SS object
	var <type;
	var <step;

	*new { |object|
		^super.new.initMovement(object)
	}

	initMovement{ |myobject|
		object=myobject;
		step=object.world.stepFreq;
	}

	next {
		// to be implemented by subclasses
	}

}

Static : Movement {

	*new { |object|
		^super.new(object).initStatic;
	}

	initStatic {
		type=\static;
	}

	next {
		// passive object
		object.vel_(object.vel+object.accel);
		object.loc_(object.loc+object.vel);
		object.accel_([0,0,0]);
		// "static".postln;
		// ^[Cartesian.new,Cartesian.new,Cartesian.new];
	}
}

RectMov : Movement {
	/*
	var vel;
	var accel;*/

	*new { |object,args|
		^super.new(object).initRect(args);
	}

	initRect { |args|
		/* vel = args[0] ? Cartesian.new;
		accel = args[1] ? Cartesian.new;*/

		if (args.size>0) { object.addVel(args[0])};

		type=\rect;

	}

	next {
		// var step=object.world.stepFreq;
		var newLoc,newVel,newAccel;

		object.vel_(object.vel+object.accel); /*.object.world.limit(world.maxVel)*/
		object.loc_(object.loc+(object.vel/step));
		object.accel_([0,0,0]);

		/* "***".postln;
		object.vel.postln;
		object.loc.postln;*/

		/* newVel=object.vel+object.accel;
		newLoc=(object.loc+(object.vel/step));
		newAccel=Cartesian[0,0,0];*/

		/* newVel=vel+accel;
		newLoc=vel/step;
		newAccel=Cartesian.new;*/

		// ^[newLoc,newVel,newAccel];
	}
}

RandomMov : Movement {

	var <>maxValue;
	var <>period;
	var count=0;

	*new { |object,args|
		^super.new(object).initRandom(args);
	}

	initRandom { |args|

		if (args.size>0) {
			maxValue = args[0];
			period = args[1];
		} {
			maxValue=1.0;
			// period=object.world.stepFreq;
			period=step;
		};
		/* vel = args[0] ? Cartesian.new;
		accel = args[1] ? Cartesian.new;*/

		// if (args.isNil.not) { object.addVel(args[0])};
		// period=object.world.stepFreq;

		type=\random;

	}

	next {
		// var step=object.world.stepFreq;
		if (count==0) {
			//new dir,vel,accel
			object.vel_(3.collect({maxValue.rand2}));
			object.accel_(3.collect({maxValue.rand2}));

			count=period;
		};
		object.vel_(object.vel+object.accel); /*.object.world.limit(world.maxVel)*/
		object.loc_(object.loc+(object.vel/step));
		// object.accel_([0,0,0]); //constant acceleration

		count=count-1;

	}

}

Brownian : Movement {

	var x,y,z;
	var dimX,dimY,dimZ;
	var <step; // percentage about 1 for the room size

	*new { |object,args|
		^super.new(object).initBrownian(args)
	}

	initBrownian { |args|

		if (args.size>0) {
			step = args[0];
		} {
			step = 0.01; // 1% of the dim size
		};

		dimX = object.world.dim.x;
		dimY = object.world.dim.y;
		dimZ = object.world.dim.z;

		x = Pbrown(dimX/2.neg, dimX/2, step*dimX).asStream;
		y = Pbrown(dimY/2.neg, dimY/2, step*dimY).asStream;
		z = Pbrown(0, dimZ, step*dimZ).asStream;

		type=\brown;
	}

	step_ { |newStep|
		step=newStep;
		x = Pbrown(dimX/2.neg, dimX/2, step*dimX).asStream;
		y = Pbrown(dimY/2.neg, dimY/2, step*dimY).asStream;
		z = Pbrown(0, dimZ, step*dimZ).asStream;
	}

	next {
		object.loc_([x.next,y.next,z.next])
	}

}



Shm : Movement {
	// TODO: with the center position saved, it is not possible to change location!
	var amp;
	var <>xAmp,<>yAmp,<>zAmp;
	var <>xT,<>yT,<>zT; //period--> take care not to put 0 !!
	var count=0;
	var center;

	*new { |object,args|
		^super.new(object).initShm(args)
	}

	initShm { |args|

		if (args.size>0) {
			args.postln;
			#xAmp,yAmp,zAmp=args[0];
			[ xAmp,yAmp,zAmp].postln;
			#xT,yT,zT=args[1];
		} {
			xAmp=1;yAmp=1;zAmp=1;
			xT=1;yT=1;zT=1;
			count=0;
		};

		center=object.loc;

		type=\shm;
	}

	next {
		var xDesp,yDesp,zDesp;
		/* count.postln;*/

		// var step=object.world.stepFreq;

		// if (count==(period*step)) {
		// count=0;
		// };

		xDesp= xAmp * sin(2*pi*(1/xT)*count/step); //sin for starting in the center position
		yDesp= yAmp * sin(2*pi*(1/yT)*count/step);
		zDesp= zAmp * sin(2*pi*(1/zT)*count/step);


		object.loc_(center+Cartesian.new(xDesp,yDesp,zDesp));
		count=count+1;
	}

}

// TODO: it is not correctly implemented!!!!

Orbit : Movement {
	// movimiento circular con respecto al centro
	// TODO: ADD Z DIMENSION
	// TODO: PUT THIS AS AN OPTION...

	var <>dir;
	var <>angularVel=0;
	var <>taccel=0;

	*new { |object, args|
		^super.new(object).initOrbit(args)
	}

	initOrbit { |args| // |velMag, dir|
		angularVel = args[0] ? 1;
		dir = args[1] ? \dex;
		type=\orbit;
	}

	next {
		// var step=object.world.stepFreq;

		angularVel= angularVel+taccel; //no need to limit to maxVel as angular
		taccel= 0;

		//set new pos
		//normalize respect real seconds
		if (dir == \lev) {
			object.locSph_(object.locSph.addAzimuth(angularVel/step));
			object.vel_([angularVel*object.loc.y.neg,angularVel*object.loc.x,object.vel.z]);
			// object.vel.postln;
		} {
			object.locSph_(object.locSph.addAzimuth(angularVel.neg/step));
			object.vel_([angularVel*object.loc.y,angularVel*object.loc.x.neg,object.vel.z]);
			// object.vel.postln;
		};

	}
}