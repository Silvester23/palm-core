package de.rwth.i9.palm.feature.researcher;

/**
 * Factory interface for features on Researcher
 * 
 * @author sigit
 *
 */
public interface ResearcherFeature
{
	public ResearcherAcademicEventTree getResearcherAcademicEventTree();

	public ResearcherApi getResearcherApi();

	public ResearcherBasicInformation getResearcherBasicInformation();

	public ResearcherCoauthor getResearcherCoauthor();

	public ResearcherInterest getResearcherInterest();
	
	public ResearcherInterestEvolution getResearcherInterestEvolution();

	public ResearcherMining getResearcherMining();

	public ResearcherPublication getResearcherPublication();

	public ResearcherSearch getResearcherSearch();

	public ResearcherTopicModeling getResearcherTopicModeling();

	public ResearcherTopPublication getResearcherTopPublication();
}
