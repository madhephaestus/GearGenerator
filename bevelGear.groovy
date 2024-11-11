import java.nio.file.Paths

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cube
import eu.mihosoft.vrl.v3d.Cylinder
import eu.mihosoft.vrl.v3d.FileUtil

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

List<Object>  makeGear(double numTeeth,double thickness,double bevelAngle,double toothBaseArchLen,double face, double helical, def pressureAngle){

	Type TT_mapStringString = new TypeToken<HashMap<String, HashMap<String, Object>>>() {}.getType();
	Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	String cacheName = 	"Gear-"+numTeeth+"-"+
			thickness+"-"+
			bevelAngle+"-"+
			toothBaseArchLen+"-"+
			face+"-"+
			helical+"-"+
			pressureAngle
//	File repoDir= ScriptingEngine.getRepositoryCloneDirectory("https://github.com/madhephaestus/GearGenerator.git")
//	File cacheDir = new File(repoDir.getAbsolutePath()+"/cache/")
//	if(!cacheDir.exists())
//		cacheDir.mkdir()
//	File cacheSTL = new File(cacheDir.getAbsolutePath()+"/"+cacheName+".stl")
//	File cachejson = new File(cacheDir.getAbsolutePath()+"/"+cacheName+".json")
//	if(cacheSTL.exists() && cachejson.exists()) {
//		println "Loading cached gear "+cacheName
//		CSG gear = Vitamins.get(cacheSTL);
//		String jsonString = null;
//		InputStream inPut = null;
//		inPut = FileUtils.openInputStream(cachejson);
//		jsonString = IOUtils.toString(inPut);
//		HashMap<String, HashMap<String, Object>> database = gson.fromJson(jsonString, TT_mapStringString);
//		HashMap<String, Object> newData = database.get("gearMetaData")
//		return [gear,newData.get("baseRad"),newData.get("toothAngle"),newData.get("toothDepth")]
//	}


	double toothAngle = (360.0)/numTeeth
	double baseThickness = toothBaseArchLen/Math.PI
	thickness-=baseThickness
	double baseDiam = (toothBaseArchLen*numTeeth)/Math.PI
	//double otherTeeth = (1.0/ratio)*numTeeth
	//double bevelAngle = Math.toDegrees(Math.atan2(otherTeeth,numTeeth))

	double topDiamOffset = thickness/(Math.tan(Math.toRadians(bevelAngle)))
	double topDiam = baseDiam-topDiamOffset*2

	//thickness-=baseThickness
	double baseDiamLower = baseDiam-(baseThickness*2* Math.cos(Math.toRadians(bevelAngle)))
	double totalThickness = thickness+baseThickness
	double toothDepth = calculateToothDepth(toothBaseArchLen)
	//println "Tooth Angle " +toothAngle+" tooth baseArchLen "+toothBaseArchLen+" base Diam "+ baseDiam+" top diam "+topDiam+" thickness "+thickness+" toothInset "+topDiamOffset
	//println " angle "+bevelAngle
	CSG upperSection =new Cylinder(baseDiam/2,topDiam/2,thickness,(int)numTeeth).toCSG() // a one line Cylinder
	CSG lowerSection =new Cylinder(baseDiamLower/2,baseDiam/2,baseThickness,(int)numTeeth).toCSG() // a one line Cylinder
			.toZMax()
	CSG blank = upperSection.union(lowerSection)
			.toZMin()
	if(bevelAngle<90)
		helical=0
	CSG toothCutter = new Cube(toothBaseArchLen,0.01,face+thickness*2).toCSG()
			.toXMin()
			.toYMin()
			.rotz(pressureAngle)
	CSG toothCutterMirror = new Cube(toothBaseArchLen,0.01,face+thickness*2).toCSG()
			.toXMin()
			.toYMin()
			.rotz(-pressureAngle)
	def cutters = [
		toothCutter,
		toothCutterMirror
	].collect{
		it.toZMax()
				.movez(thickness*1.5)
				.rotx(helical)
				.movex(-toothDepth)
				.roty(90-bevelAngle)
				.movez(totalThickness)
				.movex(topDiam/2)
	}

	double angleScale = -0.2
	double cutterOffset = toothAngle*angleScale
	toothCutter = cutters[0].rotz(-cutterOffset).union(cutters[1].rotz(cutterOffset))
			//.rotz(-toothAngle/2.0)
			.hull()
	//.movex(-toothBaseArchLen*0.1)

	for(int i=0;i<numTeeth;i++){
		blank=blank.difference(toothCutter.rotz(toothAngle*i))
	}

	FileUtil.write(Paths.get(cacheSTL.toURI()),
			blank.toStlString());
	
	HashMap<String, HashMap<String, Object>> database= new HashMap<String, HashMap<String, Object>>()
	HashMap<String, Object> newData = new HashMap<>()
	newData.put("baseRad",baseDiam/2)
	newData.put("toothAngle",toothAngle)
	newData.put("toothDepth",toothDepth)
	newData.put("stlLocation",cacheSTL.getAbsolutePath())
	database.put("gearMetaData",newData)
	String writeOut = gson.toJson(database, TT_mapStringString);
	FileUtil.write(Paths.get(cachejson.toURI()),
		writeOut);
	//println writeOut+"\r\n"+cachejson.toURI()
	return [
		blank//.union(toothCutter)
		,
		baseDiam/2,
		toothAngle,
		toothDepth
	]
}

double calculateToothDepth(double pitch){
	return pitch/Math.PI*1.5
}

List<Object>  makeRack(Number linearPitch,Number rackLength,Number toothFace,Number rackTrackCurveRadius=null,def pressureAngle){
	double baseThickness = linearPitch/Math.PI
	double toothDepth = calculateToothDepth(linearPitch)
	int numTeeth =(int) (rackLength/linearPitch)
	CSG rackBase = new Cube(1,linearPitch,toothFace).toCSG()
			.toZMin()
			.toXMin()
			.toYMin()
			.movey(-linearPitch/2)
			.movex(toothDepth)
	double toothFaceLen = (toothDepth*0.9)/Math.cos(Math.toRadians(pressureAngle))
	CSG toothCutter = new Cube(toothFaceLen,0.1,toothFace).toCSG()
			.toXMin()
			.toZMin()
			.rotz(pressureAngle)
			.movey(-linearPitch/8)
	CSG toothCutterB = new Cube(toothFaceLen,0.1,toothFace).toCSG()
			.toXMin()
			.toZMin()
			.rotz(-pressureAngle)
			.movey(linearPitch/8)
	def tooth = CSG.unionAll([toothCutter, toothCutterB])
	.movex(toothDepth*0.1)
	.hull()
	.union(rackBase)
	CSG baseOfAll = tooth.clone()
	if(rackTrackCurveRadius==null)	{
		for(int i=1;i<numTeeth;i++){
			baseOfAll=baseOfAll.union(tooth.movey(linearPitch*i))
		}
	}else{
		double centralAngle = (linearPitch/(2.0*Math.PI*rackTrackCurveRadius))*360
		def offsetTooth = tooth.movez(-rackTrackCurveRadius)
		baseOfAll = offsetTooth.clone()
		for(int i=1;i<numTeeth;i++){
			baseOfAll=baseOfAll.union(offsetTooth
					.rotx(-centralAngle*i)
					)
		}
		baseOfAll=baseOfAll.movez(rackTrackCurveRadius)
	}
	return [baseOfAll , 0, 90, toothDepth]
}

List<Object> makeBevelBox(Number numDriveTeeth,
		Number numDrivenTeeth,
		Number thickness,
		Number toothBaseArchLen,
		double axelAngleDegrees = 90,
		double helical=0,
		Number meshInterference = null,
		Number pressureAngle=null,
		Boolean makeRackFlag=false,
		Number rackTrackCurveAngle=null){

	Type TT_mapStringString = new TypeToken<HashMap<String, HashMap<String, Object>>>() {}.getType();
	Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	if(axelAngleDegrees>90)axelAngleDegrees=90
	if(axelAngleDegrees<0)axelAngleDegrees=0
	if(	 pressureAngle==null)pressureAngle=14.5
	
	String cacheName = ""+numDriveTeeth+"-"+
						numDrivenTeeth+"-"+
						thickness+"-"+
						toothBaseArchLen+"-"+
						axelAngleDegrees+"-"+
						helical+"-"+
						meshInterference+"-"+
						pressureAngle+"-"+
						makeRackFlag+"-"+
						rackTrackCurveAngle
	File repoDir= ScriptingEngine.getRepositoryCloneDirectory("https://github.com/madhephaestus/GearGenerator.git")
	File cacheDir = new File(repoDir.getAbsolutePath()+"/cache/")
	if(!cacheDir.exists())
		cacheDir.mkdir()
	File cacheSTLA = new File(cacheDir.getAbsolutePath()+"/"+cacheName+"-a.stl")
	File cacheSTLB = new File(cacheDir.getAbsolutePath()+"/"+cacheName+"-b.stl")
	File cachejson = new File(cacheDir.getAbsolutePath()+"/"+cacheName+".json")
	if(cacheSTLA.exists() && cacheSTLB.exists() &&cachejson.exists()) {
		println "Loading cached gears "+cacheName
		CSG geara = Vitamins.get(cacheSTLA);
		CSG gearb = Vitamins.get(cacheSTLB);
		
		String jsonString = null;
		InputStream inPut = null;
		inPut = FileUtils.openInputStream(cachejson);
		jsonString = IOUtils.toString(inPut);
		HashMap<String, HashMap<String, Object>> database = gson.fromJson(jsonString, TT_mapStringString);
		HashMap<String, Object> newData = database.get("gearMetaData")
		return [geara,
			gearb,
			newData.get("gearCenterToCenterX"),
			newData.get("GearCenterToAxilZ"),
			newData.get("bevelAngleInDegrees"),
			newData.get("faceLength"),
			newData.get("ThicknessOfComputedGear"),
			newData.get("GearRatio"),
			newData.get("bevelAngleOfGear-A"),
			newData.get("bevelAngleOfGear-B"),
			newData.get("TheComputedMeshInterference"),
			newData.get("ReferenceRadiusOfGear-A"),
			newData.get("ReferenceRadiusOfGear-B"),
			newData.get("TipRadiusOfaGear-A"),
			newData.get("TipRadiusOfaGear-B"),
			newData.get("stl-a-Location"),
			newData.get("stl-b-Location")
			]
//		return [gear,newData.get("baseRad"),newData.get("toothAngle"),newData.get("toothDepth")]
	}
	double baseThickness = toothBaseArchLen/Math.PI
	axelAngle=Math.toRadians(axelAngleDegrees)
	double bevelTriangleAngle = Math.PI-axelAngle
	// c² = b² + a² - 2ba cosC
	double lengthOfBevelCenter  =  Math.sqrt(
			Math.pow(numDriveTeeth,2)+
			Math.pow(numDrivenTeeth,2)-
			2.0*numDrivenTeeth*numDriveTeeth*Math.cos(bevelTriangleAngle)
			)
	//
	double Kvalue = numDrivenTeeth*numDriveTeeth*Math.sin(bevelTriangleAngle)/2.0
	double height = 2*Kvalue/lengthOfBevelCenter


	double bevelAngle =Math.acos(height/numDrivenTeeth)
	double bevelAngleB = Math.acos(height/numDriveTeeth)
	double face  = (thickness-baseThickness)/Math.sin(bevelAngle)
	double otherThick = face*Math.sin(bevelAngleB)+baseThickness
	
	//println "\n\nHeight "+(thickness-baseThickness)
	//println "Face "+face
	//println "Other Thickness "+otherThick
	def gearB = makeGear(numDrivenTeeth,otherThick,Math.toDegrees(bevelAngleB),toothBaseArchLen,face,helical,pressureAngle)
	def gearA;
	if(makeRackFlag==false)
		gearA= makeGear(numDriveTeeth,thickness,Math.toDegrees(bevelAngle),toothBaseArchLen,face,-helical,pressureAngle)
	else
		gearA=makeRack(toothBaseArchLen,toothBaseArchLen*numDriveTeeth,thickness,rackTrackCurveAngle,pressureAngle)
	if(meshInterference==null)
		meshInterference= new Double(gearA.get(3)*Math.cos(axelAngle))
	double aDiam = gearB.get(1)*Math.cos(axelAngle)+
			gearA.get(1)-meshInterference// mesh interference distance
	double bDiam = gearB.get(1)*Math.sin(axelAngle)
	double bangle = gearB.get(2)
	//println bangle
	def rotAngle = (numDriveTeeth%2==0 && numDrivenTeeth%2==0)?1:-1
	CSG gearBFinal = gearB.get(0)
			.rotz(bangle/2*rotAngle)
			.roty(Math.toDegrees(axelAngle))
			.movex(aDiam)
			.movez(bDiam)
			.rotz(180)

	CSG gearAFinal = gearA.get(0)
	double ratio = (gearA.get(1)-meshInterference/2)/(gearB.get(1)-meshInterference/2)
	
	FileUtil.write(Paths.get(cacheSTLA.toURI()),
		gearAFinal.toStlString());
	FileUtil.write(Paths.get(cacheSTLB.toURI()),
		gearBFinal.toStlString());
	HashMap<String, HashMap<String, Object>> database= new HashMap<String, HashMap<String, Object>>()
	HashMap<String, Object> newData = new HashMap<>()
	newData.put("gearCenterToCenterX",aDiam)
	newData.put("GearCenterToAxilZ",bDiam)
	newData.put("bevelAngleInDegrees",Math.toDegrees(bevelAngle))
	newData.put("faceLength",face)
	newData.put("ThicknessOfComputedGear",otherThick)
	newData.put("GearRatio",ratio)
	newData.put("bevelAngleOfGear-A",gearA.get(2))
	newData.put("bevelAngleOfGear-B",gearB.get(2))
	newData.put("TheComputedMeshInterference",meshInterference)
	newData.put("ReferenceRadiusOfGear-A",gearA.get(1))
	newData.put("ReferenceRadiusOfGear-B",gearB.get(1))
	newData.put("TipRadiusOfaGear-A",gearA.get(1)-meshInterference/2)
	newData.put("TipRadiusOfaGear-B",gearB.get(1)-meshInterference/2)
	newData.put("stl-a-Location",cacheSTLA.getAbsolutePath())
	newData.put("stl-b-Location",cacheSTLB.getAbsolutePath())
	
	//Add data to database
	database.put("gearMetaData",newData)
	String writeOut = gson.toJson(database, TT_mapStringString);
	FileUtil.write(Paths.get(cachejson.toURI()),
		writeOut);
	
	return [
		gearAFinal,
		//0 A CSG
		gearBFinal,
		//1 B CSG
		aDiam ,
		//2 Center to center in X
		bDiam,
		//3 Center to center in Y
		Math.toDegrees(bevelAngle),
		//4 Bevel angle in degrees
		face,
		//5 length of the face of the gear
		otherThick,
		//6 thickness of the computed gear
		ratio,
		//7 gear ratio
		gearA.get(2),
		//8 angle of A tooth
		gearB.get(2),
		//9 angle of B tooth
		meshInterference,
		//10 the computed mesh interference
		gearA.get(1),
		//11 reference Radius of A gear
		gearB.get(1),
		//12 reference Radius of B gear
		gearA.get(1)-meshInterference/2,
		//13 Tip Radius of A gear
		gearB.get(1)-meshInterference/2 //14 Tip Radius of B gear
	]
}
double computeGearPitch(double diameterAtCrown,double numberOfTeeth){
	return ((diameterAtCrown/2)*((360.0)/numberOfTeeth)*Math.PI/180)
}

if(args != null)
	return makeBevelBox(args)
double helical =20
// call a script from another library
def bevelGears = makeBevelBox([
	42,
	20,
	6,
	6,
	0,
	0,
	null,
	20,
	true,
	200
])

//Print parameters returned by the script
println "Bevel gear axil center to center " + bevelGears.get(2)
println "Bevel gear axil Height " + bevelGears.get(3)
println "Bevel angle " + bevelGears.get(4)
println "Bevel tooth face length " + bevelGears.get(5)
println "Gear B computed thickness " + bevelGears.get(6)
println "Gear Ratio " + bevelGears.get(7)
println "Mesh Interference calculated: " + bevelGears.get(9)
// return the CSG parts
//return bevelGears
return [
	bevelGears,
	makeBevelBox([42, 20, 6, 6, 0, 0]).collect{
		try{
			return it.movey(-100)
		}catch(Exception e){
			return it
		}
	},
	makeBevelBox([41, 22, 4, 6, 90, 0]).collect{
		try{
			return it.movey(100)
		}catch(Exception e){
			return it
		}
	}
]

