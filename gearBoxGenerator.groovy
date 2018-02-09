double computeGearPitch(double diameterAtCrown,double numberOfTeeth){
	return ((diameterAtCrown/2)*((360.0)/numberOfTeeth)*Math.PI/180)
}

if(args == null){
	args = [	new Cylinder(10,40,(int)6).toCSG(), // input shaft
			new Cylinder(10,40,(int)6).toCSG(), // output shaft
			Vitamins.get("ballBearing","R8-60355K505"),// bearing CSG
			computeGearPitch(26.15,24),// gear pitch in arc length mm
			90,// output shaft angle
			3.0,// gear ratio
			4,// number of stages
			true// extend shaft out the back
			]
}

double ratio = args.get(5)
double pitch = args.get(3)
double shaftAngle = args.get(4)
int numberOfStages = args.get(6)
boolean useOutputShaft = args.get(7)
CSG bearing = args.get(2)
CSG inShaft = args.get(0)
CSG outShaft = args.get(2)

double ratioPerStage = ratio/numberOfStages
int numTeeth =(int)((bearing.getTotalX()+pitch*2+ bearing.getTotalZ()*2)*Math.PI/pitch)+1
int numTeethSecond = numTeeth*ratioPerStage

println "Gear set "+numTeeth+" to "+numTeethSecond

def mainGears = ScriptingEngine.gitScriptRun(
            "https://github.com/madhephaestus/GearGenerator.git", // git location of the library
            "bevelGear.groovy" , // file to load
            // Parameters passed to the funcetion
            [	  numTeeth,// Number of teeth gear a
	            numTeethSecond,// Number of teeth gear b
	            3.5,// thickness of gear A
	           pitch,// gear pitch in arc length mm
	           shaftAngle,// shaft angle, can be from 0 to 100 degrees
	            0// helical angle, only used for 0 degree bevels
            ]
            )


return [bearing,mainGears]