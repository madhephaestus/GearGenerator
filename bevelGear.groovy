List<Object>  makeGear(double numTeeth,double thickness,double bevelAngle,double toothBaseArchLen ){
	double toothAngle = (360.0)/numTeeth
	double baseThickness = toothBaseArchLen/Math.PI
	thickness-=baseThickness
	double baseDiam = (toothBaseArchLen*numTeeth)/Math.PI
	//double otherTeeth = (1.0/ratio)*numTeeth
	//double bevelAngle = Math.toDegrees(Math.atan2(otherTeeth,numTeeth))

	double topDiamOffset = thickness/(Math.tan(Math.toRadians(bevelAngle)))
	double topDiam = baseDiam-topDiamOffset*2
	
	//thickness-=baseThickness
	double baseDiamLower = baseDiam-(baseThickness*2)
	double totalThickness = thickness+baseThickness
	double toothDepth = baseThickness*1.5
	//println "Tooth Angle " +toothAngle+" tooth baseArchLen "+toothBaseArchLen+" base Diam "+ baseDiam+" top diam "+topDiam+" thickness "+thickness+" toothInset "+topDiamOffset
	//println "Tooth num " +numTeeth+" other "+otherTeeth+" angle "+bevelAngle
	CSG upperSection =new Cylinder(baseDiam/2,topDiam/2,thickness,(int)numTeeth).toCSG() // a one line Cylinder
	CSG lowerSection =new Cylinder(baseDiamLower/2,baseDiam/2,baseThickness,(int)numTeeth).toCSG() // a one line Cylinder
					.toZMax()
	CSG blank = upperSection.union(lowerSection)
				.toZMin()
	CSG toothCutter = new Cube(toothBaseArchLen,toothBaseArchLen,baseDiam).toCSG()
					.toXMin()
					.toYMin()
					.rotz(45)
					.scaley(0.5)
					.toZMax()
					.movez(totalThickness)
					.movex(-toothDepth)
					//.movez(thickness)
					.roty(90-bevelAngle)
					.movez(totalThickness)
					.movex(topDiam/2)
	toothCutter = toothCutter.rotz(-toothAngle/8).union(toothCutter.rotz(toothAngle/8)).hull()
	for(int i=0;i<numTeeth;i++){
		blank=blank.difference(toothCutter.rotz(toothAngle*i))
	}
	
	return [blank,baseDiam/2,toothAngle]
}

List<Object> makeBevelBox(Number numDriveTeeth, Number numDrivenTeeth,Number thickness,Number toothBaseArchLen ){
	double axelAngle = 90
	double bevelAngle = Math.atan2(numDrivenTeeth,numDriveTeeth)
	double bevelAngleB =(Math.toRadians(axelAngle))-bevelAngle
	double face  = thickness/Math.sin(bevelAngle)
	double otherThick = face*Math.sin(bevelAngleB)
	
	def gearA = makeGear(numDriveTeeth,thickness,Math.toDegrees(bevelAngle),toothBaseArchLen)
	def gearB = makeGear(numDrivenTeeth,otherThick,Math.toDegrees(bevelAngleB),toothBaseArchLen)
	double aDiam = gearA.get(1)
	double bDiam = gearB.get(1)
	double bangle = gearA.get(2)
	CSG gearBFinal = gearB.get(0)
					.roty(axelAngle)
					.movex(aDiam)
					.movez(bDiam)
	
	return [gearA.get(0).rotz(bangle/2),gearBFinal,aDiam ,bDiam,Math.toDegrees(bevelAngle),face]
}
double computeGearPitch(double diameterAtCrown,double numberOfTeeth){
	return ((diameterAtCrown/2)*((360.0)/numberOfTeeth)*Math.PI/180)
}

if(args == null){
	args = [24,30,4.9,computeGearPitch(26.15,24)]
}
return makeBevelBox(args)