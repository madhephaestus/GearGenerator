import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cylinder

double computeGearPitch(double diameterAtCrown,double numberOfTeeth){
	return ((diameterAtCrown/2)*((360.0)/numberOfTeeth)*Math.PI/180)
}

if(args == null){
	args = [	new Cylinder(10,40,(int)6).toCSG(), // input shaft
			new Cylinder(10,40,(int)6).toCSG(), // output shaft
			Vitamins.get("ballBearing","R8-60355K505"),// bearing CSG
			computeGearPitch(26.15,24)*2,// gear pitch in arc length mm
			90,// output shaft angle
			5,// gear ratio
			1,// number of stages
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

double ratioPerStage
int additionalTeeth
int numTeeth
int numTeethSecond
double gearThickness = bearing.getTotalZ()

while(true ){
	ratioPerStage =((ratio)/numberOfStages)
	if(ratioPerStage>2){
		numberOfStages++
		continue
	}else
		break
}

numTeeth =(int)((bearing.getTotalX()+pitch*2+ bearing.getTotalZ()*2)*Math.PI/pitch)
if(numTeeth%2!=0){
	numTeeth+=1
}

additionalTeeth = ((double)numTeeth)*ratioPerStage
numTeethSecond = additionalTeeth

println ratioPerStage+" ratio on each "+numberOfStages+" Gear sets "+numTeeth+" to "+additionalTeeth

def finalGears = ScriptingEngine.gitScriptRun(
            "https://github.com/madhephaestus/GearGenerator.git", // git location of the library
            "bevelGear.groovy" , // file to load
            // Parameters passed to the funcetion
            [	  numTeeth,// Number of teeth gear a
	            numTeethSecond,// Number of teeth gear b
	           gearThickness,// thickness of gear A
	           pitch,// gear pitch in arc length mm
	           shaftAngle,// shaft angle, can be from 0 to 100 degrees
            ]
            )
CSG outputGear=finalGears.get(1)
CSG driveOutputGear=finalGears.get(0)
def gears = [outputGear]
CSG perviousInput=driveOutputGear
for(int i=1;i<numberOfStages;i++){
	def stage = ScriptingEngine.gitScriptRun(
            "https://github.com/madhephaestus/GearGenerator.git", // git location of the library
            "bevelGear.groovy" , // file to load
            // Parameters passed to the funcetion
            [	  numTeeth,// Number of teeth gear a
	            numTeethSecond,// Number of teeth gear b
	           gearThickness,// thickness of gear A
	           pitch,// gear pitch in arc length mm
	           0,// shaft angle, can be from 0 to 100 degrees
            ]
            )
    boolean odd = i%2>0.001
    
    CSG d=stage.get(0)
    			.movez(-gearThickness*i)
    CSG o=stage.get(1)
    			.movez(-gearThickness*i)
    double center = stage.get(2)
    if(odd){
    	d=d.movex(-center)
    	o=o.movex(center)
    }
    gears.add(perviousInput.union(o))

    perviousInput=d
}
gears.add(perviousInput)

return gears