List<Object>  makeGear(double numTeeth,double thickness,double bevelAngle,double toothBaseArchLen,double face, double helical){

	double pressureAngle = 25
	
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
					.movex(-toothDepth)
					.rotz(pressureAngle)
	CSG toothCutterMirror = new Cube(toothBaseArchLen,0.01,face+thickness*2).toCSG()
					.toXMin()
					.toYMin()
					.movex(-toothDepth)
					.rotz(-pressureAngle)
	def cutters = [toothCutter,toothCutterMirror].collect{
		it.movex(toothDepth)
					.toZMax()
					.movez(thickness*1.5)
					.rotx(helical)
					.movex(-toothDepth)
					//.movez(thickness)
					.roty(90-bevelAngle)
					.movez(totalThickness)
					.movex(topDiam/2)
	}
					
	double angleScale = -0.75
	double cutterOffset = toothAngle*angleScale
	toothCutter = cutters[0].union(
					cutters[1]
					.rotz(cutterOffset)
				)
				.rotz(-toothAngle/4.0)
				.hull()
				.movex(-toothBaseArchLen*0.1)

	for(int i=0;i<numTeeth;i++){
		blank=blank.difference(toothCutter.rotz(toothAngle*i))
	}


	return [blank.union(toothCutter)
     ,baseDiam/2,toothAngle,toothDepth]
}

double calculateToothDepth(double pitch){
	return pitch/Math.PI*1.5
}

List<Object>  makeRack(Number linearPitch,Number rackLength,Number toothFace,Number rackTrackCurveAngle=null){
	double baseThickness = linearPitch/Math.PI
	double toothDepth = calculateToothDepth(linearPitch)
	
	CSG rackBase = new Cube(1,rackLength,toothFace).toCSG()
				.toZMin()
				.toXMin()
				.movex(toothDepth)
	
	
	
	return [rackBase ,0,90,toothDepth]
}

List<Object> makeBevelBox(Number numDriveTeeth, 
Number numDrivenTeeth,
Number thickness,
Number toothBaseArchLen, 
double axelAngleDegrees = 90,
double helical=0,
Number meshInterference = null,
Boolean makeRackFlag=false,
Number rackTrackCurveAngle=null){
	if(axelAngleDegrees>90)axelAngleDegrees=90
	if(axelAngleDegrees<0)axelAngleDegrees=0
	double baseThickness = toothBaseArchLen/Math.PI
	axelAngle=Math.toRadians(axelAngleDegrees)
	double bevelTriangleAngle = Math.PI-axelAngle
	// c² = b² + a² - 2ba cosC
	double lengthOfBevelCenter  =  Math.sqrt(
		Math.pow(numDriveTeeth,2)+
		Math.pow(numDrivenTeeth,2)-
		2.0*numDrivenTeeth*numDriveTeeth*Math.cos(bevelTriangleAngle)
		)
	//￼￼￼
	double Kvalue = numDrivenTeeth*numDriveTeeth*Math.sin(bevelTriangleAngle)/2.0
	double height = 2*Kvalue/lengthOfBevelCenter
	
	
	double bevelAngle =Math.acos(height/numDrivenTeeth)
	double bevelAngleB = Math.acos(height/numDriveTeeth)
	double face  = (thickness-baseThickness)/Math.sin(bevelAngle)
	double otherThick = face*Math.sin(bevelAngleB)+baseThickness
	
	//println "\n\nHeight "+(thickness-baseThickness)
	//println "Face "+face
	//println "Other Thickness "+otherThick
	def gearB = makeGear(numDrivenTeeth,otherThick,Math.toDegrees(bevelAngleB),toothBaseArchLen,face,helical)
	def gearA;
	if(makeRackFlag==false)
		gearA= makeGear(numDriveTeeth,thickness,Math.toDegrees(bevelAngle),toothBaseArchLen,face,-helical)
	else
		gearA=makeRack(toothBaseArchLen,toothBaseArchLen*numDriveTeeth,thickness,rackTrackCurveAngle)
	if(meshInterference==null)
		meshInterference= new Double(gearA.get(3)*Math.cos(axelAngle))
	double aDiam = gearB.get(1)*Math.cos(axelAngle)+
				gearA.get(1)-meshInterference// mesh interference distance
	double bDiam = gearB.get(1)*Math.sin(axelAngle)
	double bangle = gearB.get(2)
	//println bangle
	def rotAngle = (numDriveTeeth%2==0 && numDrivenTeeth%2==0)?1:-1
	CSG gearBFinal = gearB.get(0)
					.rotz(bangle/2+rotAngle*(360.0)/numDrivenTeeth/4.0)
					.roty(Math.toDegrees(axelAngle))
					.movex(aDiam)
					.movez(bDiam)
					.rotz(180)

	CSG gearAFinal = gearA.get(0)
	double ratio = (gearA.get(1)-meshInterference)/(gearB.get(1)-meshInterference)
	return [gearAFinal,gearBFinal,aDiam ,bDiam,Math.toDegrees(bevelAngle),face,otherThick, ratio,gearA.get(2),gearB.get(2),meshInterference]
}
double computeGearPitch(double diameterAtCrown,double numberOfTeeth){
	return ((diameterAtCrown/2)*((360.0)/numberOfTeeth)*Math.PI/180)
}

if(args != null)
	return makeBevelBox(args)
double helical =20
// call a script from another library
def bevelGears = makeBevelBox([42,20,6,6,0,0,null,true,null])

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
return [	bevelGears,
		makeBevelBox([42,20,6,6,0,0]).collect{
			try{
				return it.movey(-100)
			}catch(Exception e){
				return it
			}
			},
		makeBevelBox([41,22,4,6,90,0]).collect{
			try{
				return it.movey(100)
			}catch(Exception e){
				return it
			}
			}]