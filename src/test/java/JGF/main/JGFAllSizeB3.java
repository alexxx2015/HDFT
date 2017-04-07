package JGF.main;
/**************************************************************************
*                                                                         *
*             Java Grande Forum Benchmark Suite - Version 2.0             *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 1999.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


import JGF.euler.*;
import JGF.jgfutil.*;
import JGF.moldyn.*;
import JGF.montecarlo.*;
import JGF.raytracer.*;
import JGF.search.*;

public class JGFAllSizeB3{

  public static void main(String argv[]){
   
    int size = 1; 

    JGFInstrumentor.printHeader(3,size);

    JGFEulerBench eb = new JGFEulerBench(); 
    eb.JGFrun(size);

    JGFMolDynBench mdb = new JGFMolDynBench();
    mdb.JGFrun(size);

    JGFMonteCarloBench mcb = new JGFMonteCarloBench();
    mcb.JGFrun(size);

    JGFRayTracerBench rtb = new JGFRayTracerBench();
    rtb.JGFrun(size);

    JGFSearchBench sb = new JGFSearchBench();
    sb.JGFrun(size);

  }
}
