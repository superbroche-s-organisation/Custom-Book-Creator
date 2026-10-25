import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.Comparator;
import javax.tools.ToolProvider;

/** Executes the actual border renderer across rounded-down GUI scales. */
public final class ScreenBorderTest {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(System.getProperty("custombook.pluginRoot","src/main/resources"));
        try(var profiles=Files.list(root)) {
            for(Path profile:profiles.filter(p->Files.isRegularFile(p.resolve("custombook.definition.yaml"))).toList()) {
                String source=Files.readString(profile.resolve("templates/custombook/screen.java.ftl"));
                int start=source.indexOf("    private void drawButtonBorder("),end=source.indexOf("    private static int parseHexColor(",start);
                String method=source.substring(start,end).replace("GuiGraphicsExtractor","Graphics").replace("GuiGraphics","Graphics");
                String fixture="""
                    import java.util.*;
                    public class BorderFixture {
                        float scale; int sx(int n){return 3+Math.round(n*scale);} int sy(int n){return 5+Math.round(n*scale);}
                        int sd(int n){return Math.max(1,Math.round(n*scale));}
                        static final class Graphics {
                            Set<Long> pixels=new HashSet<>();
                            static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
                            void fill(int x1,int y1,int x2,int y2,int color){for(int x=x1;x<x2;x++)for(int y=y1;y<y2;y++)pixels.add(key(x,y));}
                            void require(int x,int y){if(!pixels.contains(key(x,y)))throw new AssertionError("Open border at "+x+","+y);}
                        }
                        %s
                        public static void run(){
                            var s=new BorderFixture();
                            for(float scale:new float[]{.05f,.1f,.25f,.33f,.5f,.66f,.75f,1f}){
                                s.scale=scale;
                                for(int offset=0;offset<8;offset++)for(int width:new int[]{1,18,120,220,256}){
                                    var g=new Graphics();s.drawButtonBorder(g,offset,offset,width,96,0xff6d5237,1);
                                    int left=s.sx(offset),top=s.sy(offset),right=Math.max(left+1,s.sx(offset+width)),bottom=Math.max(top+1,s.sy(offset+96));
                                    for(int x=left;x<right;x++){g.require(x,top);g.require(x,bottom-1);}
                                    for(int y=top;y<bottom;y++){g.require(left,y);g.require(right-1,y);}
                                }
                            }
                        }
                    }
                    """.formatted(method);
                Path dir=Files.createTempDirectory("book-border-");
                try {
                    Path java=dir.resolve("BorderFixture.java");Files.writeString(java,fixture);
                    if(ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","21","-d",dir.toString(),java.toString())!=0)throw new AssertionError("Border fixture compilation failed");
                    try(var loader=new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()})){loader.loadClass("BorderFixture").getMethod("run").invoke(null);}
                }finally{try(var paths=Files.walk(dir)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
                System.out.println("SCREEN_BORDER_OK "+profile.getFileName());
            }
        }
    }
}
