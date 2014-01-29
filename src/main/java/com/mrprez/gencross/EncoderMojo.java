package com.mrprez.gencross;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.mrprez.gencross.disk.PluginDescriptor;
import com.mrprez.gencross.exception.PersonnageVersionException;

/**
 * @goal encode
 * 
 * @phase generate-sources
 */
public class EncoderMojo extends AbstractMojo {
	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
	
	private OutputFormat outputFormat = OutputFormat.createCompactFormat();
	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;
	private String mavenVersion;
	
	
	@Override
	public void execute() throws MojoExecutionException {
		try {
			File pluginDescriptorFile = findPluginDescriptorFile();
			if(pluginDescriptorFile == null){
				getLog().warn("Cannot find the plugin descriptor file in resources.");
				return;
			}
			PluginDescriptor pluginDescriptor = new PluginDescriptor(pluginDescriptorFile);
			if(! mavenVersion.equals(pluginDescriptor.getVersion().toString())){
				throw new PersonnageVersionException("Plugin decriptor version is different from pom version");
			}
			String gcrFileName = pluginDescriptor.getGcrFileName();
			String xmlFileName = gcrFileName.replace(".gcr", ".xml");
			File classesRep = new File(project.getBasedir(), "target/classes");
			File gcrFile = new File(classesRep, gcrFileName);
			gcrFile.getParentFile().mkdirs();
			File xmlFile = new File(pluginDescriptorFile.getParent(), xmlFileName);
			xmlFile = new File(xmlFile.getAbsolutePath());
			if(!xmlFile.exists()) {
				throw new MojoExecutionException("The file "+xmlFile+" does not exist.");
			}
			encryptXml(xmlFile, gcrFile);
		} catch (Exception e) {
			throw new MojoExecutionException("Exception during Encoder plugin execution", e);
		}
	}
	
	private void encryptXml(File xmlFile, File gcrFile) throws IOException, DocumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, PersonnageVersionException{
		Document document = loadXml(xmlFile);
		if(! document.getRootElement().attributeValue("version").equals(mavenVersion)){
			throw new PersonnageVersionException("Xml version is different from pom version");
		}
		saveGcrFile(document, gcrFile);
	}
	
	
	private void saveGcrFile(Document document, File gcrFile) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IOException{
		Cipher cipher = Cipher.getInstance("DES");
    	KeySpec key = new DESKeySpec("wofvklme".getBytes());
    	cipher.init(Cipher.ENCRYPT_MODE, SecretKeyFactory.getInstance("DES").generateSecret(key));
    	CipherOutputStream cipherOutputStream = new CipherOutputStream(new FileOutputStream(gcrFile), cipher);
    	XMLWriter writer = new XMLWriter(cipherOutputStream, outputFormat);
    	try{
    		writer.write(document);
    	}finally{
    		writer.close();
    	}
	}
	
	private Document loadXml(File file) throws IOException, DocumentException{
		InputStream inputStream = new FileInputStream(file);
		InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
		StringBuilder buffer = new StringBuilder();
		try{
			int i;
			while((i=reader.read())>=0){
				buffer.append((char)i);
			}
		}finally{
			inputStream.close();
			reader.close();
		}
		Document document = DocumentHelper.parseText(buffer.toString());
		return document;
	}
	
	private File findPluginDescriptorFile(){
		for(Object resourceElement : project.getResources()) {
			Resource resource = (Resource) resourceElement;
			File pluginDescriptorFile = new File(resource.getDirectory(), PluginDescriptor.PLUGIN_DESC_FILE_NAME);
			getLog().info("pluginDescriptorFile="+pluginDescriptorFile.getAbsolutePath());
			if(pluginDescriptorFile.exists()){
				return pluginDescriptorFile;
			}
		}
		return null;
	}

	public MavenProject getProject() {
		return project;
	}
	public void setProject(MavenProject project) {
		this.project = project;
		if(project.getVersion().endsWith(SNAPSHOT_SUFFIX)){
			this.mavenVersion = project.getVersion().substring(0, project.getVersion().length() - SNAPSHOT_SUFFIX.length());
		}else{
			this.mavenVersion = project.getVersion();
		}
	}

	
	
}
